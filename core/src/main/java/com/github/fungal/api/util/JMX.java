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

package com.github.fungal.api.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ReflectionException;

/**
 * A JMX helper class
 */
public class JMX
{

   /**
    * Constructor
    */
   private JMX()
   {
   }

   /**
    * Create a MBean representation for the object argument
    * @param obj The object
    * @return The management facade for the object
    * @exception SecurityException Thrown if there isn't sufficient permissions
    */
   public static DynamicMBean createMBean(Object obj) throws SecurityException
   {
      return createMBean(obj, "", null, null, null);
   }

   /**
    * Create a MBean representation for the object argument
    * @param obj The object
    * @param description The description for the object
    * @return The management facade for the object
    * @exception SecurityException Thrown if there isn't sufficient permissions
    */
   public static DynamicMBean createMBean(Object obj, String description) throws SecurityException
   {
      return createMBean(obj, description, null, null, null);
   }

   /**
    * Create a MBean representation for the object argument
    * @param obj The object
    * @param description The description for the object
    * @param descriptions Descriptions for the attributes and operations on the object
    * @return The management facade for the object
    * @exception SecurityException Thrown if there isn't sufficient permissions
    */
   public static DynamicMBean createMBean(Object obj, String description, Map<String, String> descriptions)
      throws SecurityException
   {
      return createMBean(obj, description, descriptions, null, null);
   }

   /**
    * Create a MBean representation for the object argument
    * @param obj The object
    * @param description The description for the object
    * @param descriptions Descriptions for the attributes and operations on the object
    * @param excludeAttributes A set of attributes that should be excluded from the management facade
    * @param excludeOperations A set of operations that should be excluded from the management facade
    * @return The management facade for the object
    * @exception SecurityException Thrown if there isn't sufficient permissions
    */
   public static DynamicMBean createMBean(Object obj, 
                                          String description,
                                          Map<String, String> descriptions,
                                          Set<String> excludeAttributes,
                                          Set<String> excludeOperations)
      throws SecurityException
   {
      if (obj == null)
         throw new IllegalArgumentException("Object is null");

      if (obj instanceof DynamicMBean)
         return (DynamicMBean)obj;

      return new ManagementDelegator(obj, description, descriptions, excludeAttributes, excludeOperations);
   }

   /**
    * Management delegator class based on reflection
    */
   static class ManagementDelegator implements DynamicMBean
   {
      private Object instance;
      private MBeanInfo info;

      /**
       * Constructor
       * @param instance The object instance
       * @param description The description for the object
       * @param descriptions Descriptions for the attributes and operations on the object
       * @param excludeAttributes A set of attributes that should be excluded from the management facade
       * @param excludeOperations A set of operations that should be excluded from the management facade
       * @exception SecurityException Thrown if there isn't sufficient permissions
       */
      public ManagementDelegator(Object instance, 
                                 String description,
                                 Map<String, String> descriptions,
                                 Set<String> excludeAttributes,
                                 Set<String> excludeOperations)
         throws SecurityException
      {
         this.instance = instance;

         List<MBeanAttributeInfo> attrs = new ArrayList<MBeanAttributeInfo>();
         List<MBeanOperationInfo> ops = new ArrayList<MBeanOperationInfo>();

         Method[] methods = instance.getClass().getMethods();
         for (Method method : methods)
         {
            if ((method.getName().startsWith("get") || method.getName().startsWith("is")) && 
                method.getParameterTypes().length == 0 &&
                !method.getDeclaringClass().equals(Object.class))
            {
               String name = method.getName().startsWith("get") ? method.getName().substring(3) :
                  method.getName().substring(2);

               boolean include = true;

               if (excludeAttributes != null)
               {
                  Iterator<String> it = excludeAttributes.iterator();
                  while (include && it.hasNext())
                  {
                     String s = it.next();
                     if (name.equalsIgnoreCase(s))
                        include = false;
                  }
               }

               if (include)
               {
                  Method setMethod = null;
                  try
                  {
                     setMethod = instance.getClass().getMethod("set" + name, method.getReturnType());
                  }
                  catch (Throwable t)
                  {
                     // Ok, no set-method
                  }

                  try
                  {
                     String desc = "";
                     if (descriptions != null && descriptions.get(name) != null)
                        desc = descriptions.get(name);

                     MBeanAttributeInfo mai = new MBeanAttributeInfo(name, desc, method, setMethod);
                     attrs.add(mai);
                  }
                  catch (Throwable t)
                  {
                     // Nothing to do
                  }
               }
            }
            else
            {
               if (!method.getName().startsWith("set") && !method.getDeclaringClass().equals(Object.class))
               {
                  String name = method.getName();
                  boolean include = true;

                  if (excludeOperations != null)
                  {
                     Iterator<String> it = excludeOperations.iterator();
                     while (include && it.hasNext())
                     {
                        String s = it.next();
                        if (name.equalsIgnoreCase(s))
                           include = false;
                     }
                  }

                  if (include)
                  {
                     try
                     {
                        String desc = "";
                        if (descriptions != null && descriptions.get(name) != null)
                           desc = descriptions.get(name);

                        MBeanParameterInfo[] signature = null;

                        if (method.getParameterTypes().length > 0)
                        {
                           signature = new MBeanParameterInfo[method.getParameterTypes().length];
                           for (int i = 0; i < method.getParameterTypes().length; i++)
                           {
                              MBeanParameterInfo pi = new MBeanParameterInfo("p" + (i + 1),
                                                                             method.getParameterTypes()[i].getName(),
                                                                             "");

                              signature[i] = pi;
                           }
                        }

                        MBeanOperationInfo moi = new MBeanOperationInfo(name,
                                                                        desc, 
                                                                        signature,
                                                                        method.getReturnType().getName(),
                                                                        MBeanOperationInfo.UNKNOWN);

                        ops.add(moi);
                     }
                     catch (Throwable t)
                     {
                        // Nothing to do
                     }
                  }
               }
            }
         }

         if (attrs.size() > 0)
            Collections.sort(attrs, new MBeanAttributeComparator());

         if (ops.size() > 0)
            Collections.sort(ops, new MBeanOperationComparator());

         this.info = new MBeanInfo(instance.getClass().getName(),
                                   description != null ? description : "",
                                   attrs.size() > 0 ? attrs.toArray(new MBeanAttributeInfo[attrs.size()]) : null,
                                   null,
                                   ops.size() > 0 ? ops.toArray(new MBeanOperationInfo[ops.size()]) : null,
                                   null);
      }

      /**
       * {@inheritDoc}
       */
      public Object getAttribute(String attribute) throws AttributeNotFoundException,
                                                          MBeanException,
                                                          ReflectionException 
      {
         if (attribute == null)
            throw new AttributeNotFoundException("Invalid attribute name: null");

         String name = attribute.substring(0, 1).toUpperCase(Locale.US);
         if (attribute.length() > 1)
            name += attribute.substring(1);

         for (MBeanAttributeInfo mai : info.getAttributes())
         {
            if (name.equals(mai.getName()))
            {
               try
               {
                  Method method = null;
                  try
                  {
                     method = instance.getClass().getMethod("get" + name, (Class[])null);
                  }
                  catch (Throwable t)
                  {
                     method = instance.getClass().getMethod("is" + name, (Class[])null);
                  }

                  return method.invoke(instance, (Object[])null);
               }
               catch (Exception e)
               {
                  throw new MBeanException(e, "Exception during setAttribute(" + attribute + ")");
               }
            }
         }

         throw new AttributeNotFoundException("Invalid attribute name: " + attribute);
      }

      /**
       * {@inheritDoc}
       */
      public AttributeList getAttributes(String[] attributes)
      {
         if (attributes != null)
         {
            AttributeList result = new AttributeList();

            for (String attr : attributes)
            {
               try
               {
                  result.add(getAttribute(attr));
               }
               catch (Throwable t)
               {
                  // Nothing to do
               }
            }

            return result;
         }

         return null;
      }

      /**
       * {@inheritDoc}
       */
      public MBeanInfo getMBeanInfo()
      {
         return info;
      }

      /**
       * {@inheritDoc}
       */
      public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException,
                                                                                          ReflectionException
      {
         for (MBeanOperationInfo moi : info.getOperations())
         {
            if (actionName.equals(moi.getName()))
            {
               try
               {
                  Class[] paramTypes = null;

                  if (signature != null && signature.length > 0)
                  {
                     List<Class<?>> l = new ArrayList<Class<?>>(signature.length);

                     for (String paramType : signature)
                     {
                        Class<?> clz = Class.forName(paramType, true, instance.getClass().getClassLoader());
                        l.add(clz);
                     }

                     paramTypes = l.toArray(new Class[l.size()]);
                  }

                  Method method = instance.getClass().getMethod(actionName, paramTypes);

                  return method.invoke(instance, params);
               }
               catch (Exception e)
               {
                  throw new MBeanException(e, "Exception during invoke(" + actionName + ", " +
                                           params + ", " + signature + ")");
               }
            }
         }

         return null;
      }

      /**
       * {@inheritDoc}
       */
      public void setAttribute(Attribute attribute) throws AttributeNotFoundException,
                                                           InvalidAttributeValueException,
                                                           MBeanException,
                                                           ReflectionException
      {
         if (attribute == null)
            throw new AttributeNotFoundException("Invalid attribute name: null");

         String name = attribute.getName().substring(0, 1).toUpperCase(Locale.US);
         if (attribute.getName().length() > 1)
            name += attribute.getName().substring(1);

         for (MBeanAttributeInfo mai : info.getAttributes())
         {
            if (name.equals(mai.getName()))
            {
               try
               {
                  Method method = null;
                  try
                  {
                     method = instance.getClass().getMethod("get" + name, (Class[])null);
                  }
                  catch (Throwable t)
                  {
                     method = instance.getClass().getMethod("is" + name, (Class[])null);
                  }

                  method = instance.getClass().getMethod("set" + name, new Class[] {method.getReturnType()});

                  method.invoke(instance, new Object[] {attribute.getValue()});
               }
               catch (Exception e)
               {
                  throw new MBeanException(e, "Exception during setAttribute(" + attribute + ")");
               }
            }
         }
      }

      /**
       * {@inheritDoc}
       */
      public AttributeList setAttributes(AttributeList attributes)
      {
         if (attributes != null)
         {
            AttributeList result = new AttributeList();

            for (Attribute attr : attributes.asList())
            {
               try
               {
                  setAttribute(attr);
                  result.add(attr);
               }
               catch (Throwable t)
               {
                  // Nothing to do
               }
            }

            return result;
         }

         return null;
      }
   }

   /**
    * Comparator for MBeanAttributeInfo sorting
    */
   static class MBeanAttributeComparator implements Comparator<MBeanAttributeInfo>
   {
      /**
       * Constructor
       */
      public MBeanAttributeComparator()
      {
      }

      /**
       * Compare
       * @param o1 The first instance
       * @param o2 The second instance
       * @return -1 if o1 is less than o2; 1 if o1 is greater than o2; otherwise 0
       */
      public int compare(MBeanAttributeInfo o1, MBeanAttributeInfo o2)
      {
         return o1.getName().compareTo(o2.getName());
      }

      /**
       * Equals
       * @param obj The other object
       * @return True if equal; otherwise false
       */
      public boolean equals(Object obj)
      {
         if (obj == null)
            return false;

         if (obj == this)
            return true;

         return obj.getClass().equals(MBeanAttributeComparator.class);
      }

      /**
       * Hash code
       * @return The value
       */
      public int hashCode()
      {
         return 42;
      }
   }

   /**
    * Comparator for MBeanOperationInfo sorting
    */
   static class MBeanOperationComparator implements Comparator<MBeanOperationInfo>
   {
      /**
       * Constructor
       */
      public MBeanOperationComparator()
      {
      }

      /**
       * Compare
       * @param o1 The first instance
       * @param o2 The second instance
       * @return -1 if o1 is less than o2; 1 if o1 is greater than o2; otherwise 0
       */
      public int compare(MBeanOperationInfo o1, MBeanOperationInfo o2)
      {
         int result = o1.getName().compareTo(o2.getName());

         if (result == 0)
         {
            int p1 = o1.getSignature().length;
            int p2 = o2.getSignature().length;

            if (p1 < p2)
            {
               return -1;
            }
            else if (p1 > p2)
            {
               return 1;
            }
            else
            {
               for (int i = 0; i < o1.getSignature().length; i++)
               {
                  MBeanParameterInfo pi1 = o1.getSignature()[i];
                  MBeanParameterInfo pi2 = o2.getSignature()[i];

                  result = pi1.getType().compareTo(pi2.getType());

                  if (result != 0)
                     return result;
               }

               return 0;
            }
         }

         return result;
      }

      /**
       * Equals
       * @param obj The other object
       * @return True if equal; otherwise false
       */
      public boolean equals(Object obj)
      {
         if (obj == null)
            return false;

         if (obj == this)
            return true;

         return obj.getClass().equals(MBeanOperationComparator.class);
      }

      /**
       * Hash code
       * @return The value
       */
      public int hashCode()
      {
         return 42;
      }
   }
}