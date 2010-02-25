/*
 * Copyright (C) 2009-2010 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.transfer.manifest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.MLText;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.TempFileProvider;

import junit.framework.TestCase;

public class TransferManifestTest extends TestCase 
{
    /**
     * This unit test creates and reads a manifest.
     * 
     * @throws Exception
     */
    public void testCreateAndReadSnapshot() throws Exception
    {
        /**
         * create snapshot
         */
        String prefix = "TRX-SNAP";
        String suffix = ".xml";

        // where to put snapshot ?
        File snapshotFile = TempFileProvider.createTempFile(prefix, suffix);
        FileWriter snapshotWriter = new FileWriter(snapshotFile);
        
        // Write the manifest file
        TransferManifestWriter formatter = new XMLTransferManifestWriter();
        TransferManifestHeader header = new TransferManifestHeader();
        header.setCreatedDate(new Date());
        formatter.startTransferManifest(snapshotWriter);
        formatter.writeTransferManifestHeader(header);
        
        // node to transmit
        TransferManifestNormalNode node = new TransferManifestNormalNode();
        NodeRef nodeRefA = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "123");
        node.setNodeRef(nodeRefA); 
        node.setParentPath(new Path());
        Set<QName> aspects = new HashSet<QName>();
        aspects.add(QName.createQName("{gsxhjsx}", "cm:wobble"));
        aspects.add(QName.createQName("{gsxhjsx}", "cm:wibble"));
        node.setAspects(aspects);
        
        Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
        
        /**
         * String type
         */
        properties.put(QName.createQName("{gsxhjsx}", "cm:name"), "brian.jpg");
        
        /**
         * Date type
         */
        properties.put(QName.createQName("{gsxhjsx}", "cm:created"), new java.util.Date());
        
        /**
         * Boolean type
         */
        properties.put(QName.createQName("{gsxhjsx}", "trx:enabled"), Boolean.FALSE);
        
        /**
         * MLText value
         */
        MLText mltext = new MLText();
        mltext.addValue(Locale.FRENCH, "Bonjour");
        mltext.addValue(Locale.ENGLISH, "Hello");
        mltext.addValue(Locale.ITALY, "Buongiorno");
        properties.put(QName.createQName("{gsxhjsx}", "cm:title"), mltext);
        String password = "helloWorld";
        
        /**
         * Put a char array type
         */
        properties.put(QName.createQName("{gsxhjsx}", "trx:password"), password.toCharArray());
        
        /**
         * Put an ArrayList type
         */
        ArrayList a1 = new ArrayList();
        a1.add("Rhubarb");
        a1.add("Custard");
        properties.put(QName.createQName("{gsxhjsx}", "trx:arrayList"), a1);
        
        /**
         * Put a HashMap type
         */
        HashMap m1 = new HashMap();
        m1.put("Rhubarb", "Rhubarb");
        m1.put("Custard", "Custard");
        properties.put(QName.createQName("{gsxhjsx}", "trx:hashMap"), m1);
        
        /**
         * Put a null value
         */
        properties.put(QName.createQName("{gsxhjsx}", "cm:nullTest"), null);
        
        /**
         * Put a node ref property
         */
        properties.put(QName.createQName("{gsxhjsx}", "trx:nodeRef"), new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "P1"));
        
        /**
         * Put an obscure "unknown type".
         */
//        TestPrivateBean obscure = new TestPrivateBean();
//        obscure.a = "hello";
//        obscure.b = "world";
//        properties.put(QName.createQName("{gsxhjsx}", "cm:obscure"), obscure);
        
        List<ChildAssociationRef> parents = new ArrayList<ChildAssociationRef>();
        ChildAssociationRef primaryParent = new ChildAssociationRef(QName.createQName("{gsxhjsx}", "cm:contains"),
                new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "P1"),
                QName.createQName("{gsxhjsx}", "app:smashing"),
                new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "123"),
                true,
                -1); 
        parents.add(primaryParent);
        parents.add(new ChildAssociationRef(QName.createQName("{gsxhjsx}", "app:wibble"),
                    new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "P1"),
                    QName.createQName("{gsxhjsx}", "app:jskjsdc"),
                    new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "123"),
                    false,
                    -1));
        node.setParentAssocs(parents);
        node.setPrimaryParentAssoc(primaryParent);
        
        List<ChildAssociationRef> children = new ArrayList<ChildAssociationRef>();
        children.add(new ChildAssociationRef(QName.createQName("{gsxhjsx}", "cm:contains"),
                    new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "P1"),
                    QName.createQName("{gsxhjsx}", "app:super"),
                    new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "P5"),
                    true,
                    -1));
        
        node.setChildAssocs(children);
        
        Set<String>values=new HashSet<String>();
        values.add("red");
        values.add("blue");
        values.add("green");
        properties.put(QName.createQName("{gsxhjsx}", "xyz:colours"), (Serializable)values);
        
        ContentData contentHeader = new ContentData("http://wibble", "mimeType", 123, "utf-8", Locale.ENGLISH);
        properties.put(QName.createQName("{gsxhjsx}", "cm:content"), (Serializable)contentHeader);
        
        node.setProperties(properties);
        
        node.setType(QName.createQName("{gsxhjsx}", "trx:nsbbmbs"));
        
        List<AssociationRef> targetAssocs = new ArrayList<AssociationRef>();
        List<AssociationRef> sourceAssocs = new ArrayList<AssociationRef>();
        
        targetAssocs.add(new AssociationRef(null, new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "SA"),
                    QName.createQName("{gsxhjsx}", "app:super"),
                    new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "TA")));
        

        
        sourceAssocs.add(new AssociationRef(null, new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "HH"),
                    QName.createQName("{gsxhjsx}", "app:super"),
                    new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "JJ")));
        
        node.setSourceAssocs(sourceAssocs);
        node.setTargetAssocs(targetAssocs);
        
        formatter.writeTransferManifestNode(node);
        
        /**
         * Write a second node
         */
        
        TransferManifestNormalNode node2 = new TransferManifestNormalNode();
        node2.setNodeRef(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "456"));
        node2.setType(QName.createQName("{gsxhjsx}", "trx:dummy"));
        formatter.writeTransferManifestNode(node2);
        
        
        /**
         * Write a deleted node
         */
        TransferManifestDeletedNode node3 = new TransferManifestDeletedNode();
        node3.setNodeRef(new NodeRef(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE, "567"));
        
        ChildAssociationRef origPrimaryParent = new ChildAssociationRef(QName.createQName("{gsxhjsx}", "cm:contains"),
                new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "P1"),
                QName.createQName("{gsxhjsx}", "app:whopper"),
                new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "567"),
                true,
                -1); 

        node3.setPrimaryParentAssoc(origPrimaryParent);
        node3.setParentPath(new Path());
        
        formatter.writeTransferManifestNode(node3);
        

        formatter.endTransferManifest();
        snapshotWriter.close();
        
        // 
        BufferedReader reader = new BufferedReader(new FileReader(snapshotFile));
        String s = reader.readLine();
        while(s != null)
        {
            System.out.println(s);
            s = reader.readLine();
        }
        
        // Now try to parse the snapshot file we have just created
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        SAXParser parser = saxParserFactory.newSAXParser();
        
        TestTransferManifestProcessor tp = new TestTransferManifestProcessor(); 
        
        XMLTransferManifestReader xmlReader = new  XMLTransferManifestReader(tp);
        parser.parse(snapshotFile, xmlReader );
        
        /**
         * Now validate the parsed data.
         */
        
        Map<NodeRef, TransferManifestNode> nodes = tp.getNodes();
        
        TransferManifestNormalNode rxNodeA = (TransferManifestNormalNode)nodes.get(nodeRefA);
        
        assertNotNull("rxNodeA is null", rxNodeA);
        
        Map<QName, Serializable> rxNodeAProps = rxNodeA.getProperties();
        System.out.println(rxNodeAProps.get(QName.createQName("{gsxhjsx}", "trx:password")));
        for(Map.Entry value : rxNodeAProps.entrySet())
        {
            System.out.println("key = " + value.getKey() + " value =" + value.getValue());
            if(value.getValue() != null)
            {
                if(value.getValue().getClass().isArray())
                {
                    System.out.println("arrayValue="+ value.getValue().toString());
                    char[] chars = (char[])value.getValue();
                    
                    System.out.println(chars);
                }
            }
            
        }
                
        tp.getHeader();
        
        snapshotFile.delete();
    }
    
    public class TestPrivateBean implements Serializable
    {
        public TestPrivateBean()
        {
            
        }
        
        public void setA(String a)
        {
            this.a = a;
        }
        public String getA()
        {
            return a;
        }

        /**
         * 
         */
        private static final long serialVersionUID = 1053132227110567282L;
        private String a;
        private String b;
        
        
    }
    
}
