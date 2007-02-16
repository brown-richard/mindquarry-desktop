/*
 * Copyright (C) 2006-2007 MindQuarry GmbH, All Rights Reserved
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 */
package com.mindquarry.client.workspace.xml;

import java.util.ArrayList;
import java.util.List;

import org.dom4j.Element;
import org.dom4j.Node;

import dax.Path;
import dax.Transformer;

/**
 * @author <a href="mailto:alexander(dot)saar(at)mindquarry(dot)com">Alexander
 *         Saar</a>
 */
public class TeamListTransformer extends Transformer {
    private List<String> teamspaces = new ArrayList<String>();

    @Override
    public void init() {
        teamspaces.clear();
    }

    @Path("//teamspace")
    public void teamspace(Node node) {
        applyTemplates(node);
        if (node instanceof Element) {
            Element element = (Element) node;
            teamspaces.add(element.attribute("href").getStringValue()); //$NON-NLS-1$
        }
    }

    public List<String> getTeamspaces() {
        return teamspaces;
    }
}