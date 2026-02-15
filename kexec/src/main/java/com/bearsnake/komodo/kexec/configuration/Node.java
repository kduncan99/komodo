/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration;

import java.util.*;

public class Node {

    private final String _name;
    private final EquipType _equipType;
    private final List<Node> _subordinates = new LinkedList<>();
    private final Map<String, String> _arguments = new HashMap<>();

    public Node(
        final String name,
        final EquipType equipType
    ) {
        _name = name;
        _equipType = equipType;
    }

    public void addArgument(final String key, final String value) { _arguments.put(key, value); }
    public void addSubordinate(final Node node) { _subordinates.add(node); }
    public String getArgument(final String key) { return _arguments.get(key); }
    public EquipType getEquipType() { return _equipType; }
    public String getName() { return _name; }
    public Collection<Node> getSubordinates() { return _subordinates; }

    public static Node createNode(
        final String name,
        final String typeString
    ) {
        var equipType = EquipType.getFromToken(typeString.toUpperCase());
        if (equipType == null) {
            return null;
        }
        return new Node(name, equipType);
    }
}
