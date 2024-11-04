/*
 * The Fungal kernel project
 * Copyright (C) 2011
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

package com.github.fungal.api.deployment;

/**
 * Represents a start element
 */
public class Start
{
   /** The method attribute */
   private String method;

   /**
    * Constructor
    */
   public Start()
   {
      this.method = null;
   }

   /**
    * Get the method
    * @return The value
    */
   public String getMethod()
   {
      return method;
   }

   /**
    * Set the method
    * @param value The value
    */
   public void setMethod(String value)
   {
      method = value;
   }

   /**
    * String representation
    * @return The string
    */
   @Override
   public String toString()
   {
      StringBuilder sb = new StringBuilder();

      sb.append("StartType@").append(Integer.toHexString(System.identityHashCode(this)));
      sb.append("[method=").append(method);
      sb.append("]");

      return sb.toString();
   }
}
