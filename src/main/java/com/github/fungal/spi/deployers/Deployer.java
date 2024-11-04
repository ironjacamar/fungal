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

package com.github.fungal.spi.deployers;

import java.net.URL;

/**
 * The deployer interface for Fungal
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public interface Deployer
{
   /**
    * Accepts the URL of the deployment
    * @param deployment The URL
    * @return True if the deployer accepts the deployment; otherwise false
    */
   public boolean accepts(URL deployment);

   /**
    * Get the order for the deployer. The lower the number the sooner
    * the deployer will be scheduled in the deployment chain
    * @return The value
    */
   public int getOrder();

   /**
    * Deploy
    * @param url The URL
    * @param context The deployment context
    * @param parent The parent classloader
    * @return The deployment; or null if no deployment was made
    * @exception DeployException Thrown if an error occurs during deployment
    */
   public Deployment deploy(URL url, Context context, ClassLoader parent) throws DeployException;
}
