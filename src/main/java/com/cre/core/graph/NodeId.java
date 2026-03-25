package com.cre.core.graph;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;

/**
 * Stable identity for graph members derived from declared structure only (no line/offset).
 */
public final class NodeId implements Comparable<NodeId> {

  private static final Comparator<NodeId> ORDER =
      Comparator.comparing(NodeId::fullyQualifiedType)
          .thenComparing(NodeId::memberSignature)
          .thenComparing(NodeId::sourceOrigin);

  private final String fullyQualifiedType;
  private final String memberSignature;
  private final String sourceOrigin;

  public NodeId(String fullyQualifiedType, String memberSignature, String sourceOrigin) {
    this.fullyQualifiedType = Objects.requireNonNull(fullyQualifiedType, "fullyQualifiedType");
    this.memberSignature = Objects.requireNonNull(memberSignature, "memberSignature");
    this.sourceOrigin = Objects.requireNonNull(sourceOrigin, "sourceOrigin");
  }

  public String fullyQualifiedType() {
    return fullyQualifiedType;
  }

  public String memberSignature() {
    return memberSignature;
  }

  public String sourceOrigin() {
    return sourceOrigin;
  }

  public static String normalizeOrigin(Path path) {
    return path.toString().replace('\\', '/');
  }

  /**
   * Inverse of {@link #toString()} — three segments separated by {@code ::}.
   */
  public static NodeId parse(String raw) {
    String[] p = raw.split("::", -1);
    if (p.length != 3) {
      throw new IllegalArgumentException("Expected FQN::member::origin, got: " + raw);
    }
    return new NodeId(p[0], p[1], p[2]);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NodeId nodeId)) {
      return false;
    }
    return fullyQualifiedType.equals(nodeId.fullyQualifiedType)
        && memberSignature.equals(nodeId.memberSignature)
        && sourceOrigin.equals(nodeId.sourceOrigin);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fullyQualifiedType, memberSignature, sourceOrigin);
  }

  @Override
  public int compareTo(NodeId o) {
    return ORDER.compare(this, o);
  }

  @Override
  public String toString() {
    return fullyQualifiedType + "::" + memberSignature + "::" + sourceOrigin;
  }
}
