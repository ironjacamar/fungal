/*
 * The Fungal kernel project
 * Copyright (C) 2010
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package com.github.fungal.impl;

import com.github.fungal.api.deployment.BeanDeployment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A bean deployment for Fungal
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public class BeanDeploymentImpl implements BeanDeployment
{
   /** The deployment */
   private URL deployment;

   /** The bean names */
   private List<String> beans;

   /** Uninstall methods */
   private Map<String, List<Method>> uninstall;

   /** Stop */
   private Map<String, String> stops;

   /** Destroy */
   private Map<String, String> destroys;

   /** Ignore stop */
   private Set<String> ignoreStops;

   /** Ignore destroy */
   private Set<String> ignoreDestroys;

   /** The kernel */
   private KernelImpl kernel;

   /**
    * Constructor
    * @param deployment The deployment
    * @param beans The list of bean names for the deployment
    * @param uninstall Uninstall methods for beans
    * @param stops Stop methods for beans
    * @param destroys Destroy methods for beans
    * @param ignoreStops Ignore stop methods for beans
    * @param ignoreDestroys Ignore destroy methods for beans
    * @param kernel The kernel
    */
   public BeanDeploymentImpl(URL deployment, 
                             List<String> beans, 
                             Map<String, List<Method>> uninstall,
                             Map<String, String> stops,
                             Map<String, String> destroys,
                             Set<String> ignoreStops,
                             Set<String> ignoreDestroys,
                             KernelImpl kernel)
   {
      if (beans == null)
         throw new IllegalArgumentException("Beans is null");

      if (uninstall == null)
         throw new IllegalArgumentException("Uninstall is null");

      if (kernel == null)
         throw new IllegalArgumentException("Kernel is null");

      this.deployment = deployment;
      this.beans = beans;
      this.uninstall = uninstall;
      this.stops = stops;
      this.destroys = destroys;
      this.ignoreStops = ignoreStops;
      this.ignoreDestroys = ignoreDestroys;
      this.kernel = kernel;
   }

   /**
    * Get the bean names
    * @return The values
    */
   public List<String> getBeans()
   {
      return beans;
   }

   /**
    * Get the unique URL for the deployment
    * @return The URL
    */
   public URL getURL()
   {
      return deployment;
   }

   /**
    * Get the classloader
    * @return The classloader
    */
   public ClassLoader getClassLoader()
   {
      ClassLoader cl = null;

      if (beans != null && beans.size() > 0)
      {
         Object bean = kernel.getBean(beans.get(0));

         if (bean != null)
            cl = SecurityActions.getClassLoader(bean.getClass());
      }

      if (cl == null)
         cl = kernel.getKernelClassLoader();

      return cl;
   }

   /**
    * Stop
    * @exception Throwable If the unit cant be stopped
    */
   public void stop() throws Throwable
   {
      Set<String> remaining = new HashSet<String>();
      remaining.addAll(beans);

      for (String bean : beans)
      {
         Set<String> dependants = kernel.getBeanDependants(bean);

         if (dependants != null)
         {
            for (String dependant : dependants)
            {
               remaining.remove(dependant);
            }
         }

         remaining.remove(bean);
      }

      if (remaining.size() > 0)
         throw new Exception("Cannot stop deployment " + deployment + " due to remaining dependants " + remaining);
   }

   /**
    * Destroy
    * @exception Throwable If the unit cant be stopped
    */
   public void destroy() throws Throwable
   {
      Throwable throwable = null;

      List<String> shutdownBeans = new LinkedList<String>(beans);
      Collections.reverse(shutdownBeans);

      for (String name : shutdownBeans)
      {
         try
         {
            kernel.setBeanStatus(name, ServiceLifecycle.STOPPING);

            Object bean = kernel.getBean(name);

            if (bean != null)
            {
               List<Method> l = uninstall.get(name);
               if (l != null)
               {
                  for (Method m : l)
                  {
                     try
                     {
                        SecurityActions.setAccessible(m);
                        m.invoke(bean, (Object[])null);
                     }
                     catch (InvocationTargetException ite)
                     {
                        if (throwable == null)
                           throwable = ite.getTargetException();
                     }
                  }
               }

               if (ignoreStops == null || !ignoreStops.contains(name))
               {
                  try
                  {
                     String methodName = "stop";
                     if (stops != null && stops.containsKey(name))
                        methodName = stops.get(name);

                     Method stopMethod = SecurityActions.getMethod(bean.getClass(), methodName, (Class[])null);
                     SecurityActions.setAccessible(stopMethod);
                     stopMethod.invoke(bean, (Object[])null);
                  }
                  catch (NoSuchMethodException nsme)
                  {
                     // No stop method
                  }
                  catch (InvocationTargetException ite)
                  {
                     if (throwable == null)
                        throwable = ite.getTargetException();
                  }
               }

               if (ignoreDestroys == null || !ignoreDestroys.contains(name))
               {
                  try
                  {
                     String methodName = "destroy";
                     if (destroys != null && destroys.containsKey(name))
                        methodName = destroys.get(name);

                     Method destroyMethod = SecurityActions.getMethod(bean.getClass(), methodName, (Class[])null);
                     SecurityActions.setAccessible(destroyMethod);
                     destroyMethod.invoke(bean, (Object[])null);
                  }
                  catch (NoSuchMethodException nsme)
                  {
                     // No destroy method
                  }
                  catch (InvocationTargetException ite)
                  {
                     if (throwable == null)
                        throwable = ite.getTargetException();
                  }
               }
            }
         }
         catch (Throwable t)
         {
            if (throwable == null)
               throwable = t;
         }
         finally
         {
            kernel.removeBean(name);
         }
      }

      if (throwable != null)
         throw throwable;
   }
}
