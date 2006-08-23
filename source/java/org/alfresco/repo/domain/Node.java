/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.domain;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

/**
 * Interface for persistent <b>node</b> objects.
 * <p>
 * Specific instances of nodes are unique, but may share GUIDs across stores.
 * 
 * @author Derek Hulley
 */
public interface Node
{
    /**
     * Convenience method to get the reference to the node
     * 
     * @return Returns the reference to this node
     */
    public NodeRef getNodeRef();
    
    /**
     * @return Returns the auto-generated ID
     */
    public Long getId();
    
    public Store getStore();
    
    public void setStore(Store store);
    
    public String getUuid();
    
    public void setUuid(String uuid);
    
    public QName getTypeQName();
    
    public void setTypeQName(QName typeQName);

    public Set<QName> getAspects();
    
    public Collection<ChildAssoc> getParentAssocs();

    public Map<QName, PropertyValue> getProperties();

    public DbAccessControlList getAccessControlList();

    public void setAccessControlList(DbAccessControlList accessControlList);
}
