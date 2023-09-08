/*
 * Copyright 2023 Datastrato.
 * This software is licensed under the Apache License version 2.
 */

package com.datastrato.graviton.proto;

import com.datastrato.graviton.meta.AuditInfo;
import com.datastrato.graviton.meta.CatalogEntity;

/** A class for serializing and deserializing CatalogEntity objects using Protocol Buffers. */
public class CatalogEntitySerDe implements ProtoSerDe<CatalogEntity, Catalog> {

  /**
   * Serializes a {@link CatalogEntity} object to a {@link Catalog} object.
   *
   * @param catalogEntity The CatalogEntity object to be serialized.
   * @return The serialized Catalog object.
   */
  @Override
  public Catalog serialize(CatalogEntity catalogEntity) {
    Catalog.Builder builder =
        Catalog.newBuilder()
            .setId(catalogEntity.id())
            .setName(catalogEntity.name())
            .setAuditInfo(new AuditInfoSerDe().serialize((AuditInfo) catalogEntity.auditInfo()));

    if (catalogEntity.getComment() != null) {
      builder.setComment(catalogEntity.getComment());
    }

    if (catalogEntity.getProperties() != null && !catalogEntity.getProperties().isEmpty()) {
      builder.putAllProperties(catalogEntity.getProperties());
    }

    com.datastrato.graviton.proto.Catalog.Type type =
        com.datastrato.graviton.proto.Catalog.Type.valueOf(catalogEntity.getType().name());
    builder.setType(type);

    // Note we have ignored the namespace field here
    return builder.build();
  }

  /**
   * Deserializes a {@link Catalog} object to a {@link CatalogEntity} object.
   *
   * @param p The serialized Catalog object.
   * @return The deserialized CatalogEntity object.
   */
  @Override
  public CatalogEntity deserialize(Catalog p) {
    CatalogEntity.Builder builder = new CatalogEntity.Builder();
    builder
        .withId(p.getId())
        .withName(p.getName())
        .withAuditInfo(new AuditInfoSerDe().deserialize(p.getAuditInfo()));

    if (p.hasComment()) {
      builder.withComment(p.getComment());
    }

    if (p.getPropertiesCount() > 0) {
      builder.withProperties(p.getPropertiesMap());
    }

    builder.withType(com.datastrato.graviton.Catalog.Type.valueOf(p.getType().name()));
    return builder.build();
  }
}