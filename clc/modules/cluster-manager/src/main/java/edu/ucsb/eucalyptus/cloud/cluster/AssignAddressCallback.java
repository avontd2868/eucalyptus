/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.constants.VmState;
import edu.ucsb.eucalyptus.msgs.*;

import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.util.EucalyptusClusterException;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.client.Client;

import org.apache.log4j.Logger;

public class AssignAddressCallback extends QueuedEventCallback<AssignAddressType> {
  private static Logger LOG = Logger.getLogger( AssignAddressCallback.class );

  private VmInstance parent;
  public AssignAddressCallback( final VmInstance parent ) {
    this.parent = parent;
  }

  @Override
  public void prepare( AssignAddressType msg ) throws Exception {
    try {
      VmInstance vm = VmInstances.getInstance( ).lookup( msg.getInstanceId( ) );
      VmState vmState = vm.getState( );
      if( !VmState.RUNNING.equals( vmState ) && !VmState.PENDING.equals( vmState ) ) {
        throw new EucalyptusClusterException( "Ignoring assignment to a vm which is not running: " + msg  );
      } else {
        for( VmInstance aVm : VmInstances.getInstance( ).listValues( ) ) {
          if( msg.getSource().equals( aVm.getNetworkConfig( ).getIgnoredPublicIp( ) ) && !vm.getInstanceId( ).equals( aVm.getInstanceId( ) ) 
              && VmState.RUNNING.equals( aVm.getState( ) ) && VmState.PENDING.equals( aVm.getState( ) ) ) {
            throw new EucalyptusClusterException( "Assign [" + msg.getSource() + "]  discarded because another VM already owns this IP." );
          }
        }
        LOG.debug( "Assign [" + msg.getSource() + "]  => [" + msg.getDestination() + "]" );
        this.parent.getNetworkConfig().setIgnoredPublicIp( msg.getSource() );
      }
    } catch ( Exception e ) {
      LOG.debug( e, e );
      throw e;
    }
  }

  @Override
  public void verify( EucalyptusMessage msg ) throws Exception {}

  @Override
  public void fail( Throwable e ) {
    LOG.debug( LogUtil.subheader( this.getRequest( ).toString( "eucalyptus_ucsb_edu" ) ) );
    LOG.debug( e, e );
  }

}
