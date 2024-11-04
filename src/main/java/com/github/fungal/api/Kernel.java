/*
 * The Fungal kernel project
 * Copyright (C) 2012
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

package com.github.fungal.api;

import com.github.fungal.api.classloading.KernelClassLoader;
import com.github.fungal.api.deployer.MainDeployer;
import com.github.fungal.api.deployment.Bean;
import com.github.fungal.api.deployment.BeanDeployment;
import com.github.fungal.spi.deployers.Deployment;

import java.net.URL;
import java.util.Collection;
import java.util.List;

import javax.management.MBeanServer;

/**
 * The kernel API
 *
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 * @see com.github.fungal.api.classloading.KernelClassLoader
 * @see com.github.fungal.api.deployment.Bean
 * @see com.github.fungal.api.deployer.MainDeployer
 * @see com.github.fungal.spi.deployers.Deployment
 */
public interface Kernel
{
   /**
    * Get the name of the kernel
    * @return The value
    */
   public String getName();

   /**
    * Get the version of the kernel
    * @return The value
    */
   public String getVersion();

   /**
    * Get the MBeanServer for the kernel
    * @return The MBeanServer instance
    */
   public MBeanServer getMBeanServer();

   /**
    * Get the MainDeployer for the kernel
    * @return The MainDeployer instance
    */
   public MainDeployer getMainDeployer();

   /**
    * Get the kernel class loader
    * @return The class loader
    */
   public KernelClassLoader getKernelClassLoader();

   /**
    * Get the deployments
    * @return The deployment units; <code>null</code> if no units is found
    */
   public Collection<Deployment> getDeployments();

   /**
    * Get the deployment units for a deployment
    * @param deployment The unique URL for a deployment
    * @return The deployment units; <code>null</code> if no units is found
    */
   public List<Deployment> getDeployments(URL deployment);

   /**
    * Get a bean
    * @param name The bean name
    * @param expectedType The expected type for the bean
    * @return The bean instance
    * @exception Throwable If an error occurs
    */
   public <T> T getBean(String name, Class<T> expectedType) throws Throwable;

   /**
    * Install beans
    * @param beans The beans
    * @return The deployment that represents the beans
    * @exception Throwable If an error occurs
    */
   public BeanDeployment install(Bean... beans) throws Throwable;

   /**
    * Uninstall beans
    * @param beans The bean deployment
    * @exception Throwable If an error occurs
    */
   public void uninstall(BeanDeployment beans) throws Throwable;

   /**
    * Startup
    * @exception Throwable Thrown if an error occurs
    */
   public void startup() throws Throwable;

   /**
    * Shutdown
    * @exception Throwable Thrown if an error occurs
    */
   public void shutdown() throws Throwable;
}
