package io.vacco.myrmica.maven;

import org.joox.Match;

import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import static io.vacco.myrmica.maven.Constants.*;
import static java.lang.String.format;

public class Artifact implements Comparable<Artifact> {

  private final Match xml;
  private final Coordinates at;
  private final Component metadata;
  private final boolean optional;
  private String scope;

  private final Set<Artifact> exclusions = new TreeSet<>();

  public Artifact(Match xml) {
    this.xml = Objects.requireNonNull(xml);
    this.at = new Coordinates(xml);
    this.scope = xml.child(Constants.PomTag.scope.toString()).text();
    this.optional = Boolean.parseBoolean(xml.child(Constants.PomTag.optional.toString()).text());
    this.exclusions.addAll(artifactsOf(xml.child(Constants.PomTag.exclusions.toString())));

    Optional<Component> c = Component.forType(xml.child(ComponentTag.type.toString()).text());
    if (!c.isPresent()) { c = Component.forPackaging(xml.child(ComponentTag.packaging.toString()).text()); }
    if (!c.isPresent()) {
      c = Component.forType(Constants.DEFAULT_ARTIFACT_TYPE);
      c.get().setClassifier(xml);
    }
    this.metadata = c.get();
    if (scope == null) { scope = scope_compile; }
  }

  public String getBaseArtifactName() {
    return format("%s%s", at != null ? at.getBaseResourceName() : "",
        metadata != null && metadata.classifier != null ? format("-%s", metadata.classifier) : "");
  }

  public String toExternalForm() {
    return format("%s/%s %s%s", at.toExternalForm(), getBaseArtifactName(),
        metadata != null ? metadata.toExternalForm() : "",
        scope != null ? format(", %s", scope) : "");
  }

  public URI getResourceUri(URI origin, String resourceExtension) {
    return at.getBaseUri(origin).resolve(format("%s%s", getBaseArtifactName(), resourceExtension));
  }

  public URI getPackageUri(URI origin) { return getResourceUri(origin, format(".%s", metadata.extension)); }
  public Path getLocalPackagePath(Path root) { return Paths.get(getPackageUri(root.toUri())); }
  public boolean isRuntimeClassPath() {
    if (metadata.type.contains(scope_test)) return false;
    if (metadata.classifier != null && metadata.classifier.contains(scope_test)) return false;
    if (optional) return false;
    if (scope_provided.equals(scope)) return false;
    boolean isRtScope = scope_compile.equals(scope) || scope_runtime.equals(scope);
    return metadata.addedToClasspath && isRtScope;
  }

  @Override public int compareTo(Artifact o) { return toExternalForm().compareTo(o.toExternalForm()); }
  @Override public int hashCode() { return toExternalForm().hashCode(); }
  @Override public boolean equals(Object o) {
    if (o instanceof Artifact) {
      Artifact a0 = (Artifact) o;
      String ef0 = toExternalForm();
      String ef1 = a0.toExternalForm();
      return ef0.equals(ef1);
    }
    return false;
  }

  public Coordinates getAt() { return at; }
  public Set<Artifact> getExclusions() { return exclusions; }
  public Component getMetadata() { return metadata; }

  public String getScope() { return scope; }
  public void setScope(String scope) { this.scope = scope; }

  public boolean excludes(Artifact a) {
    boolean excluded = exclusions.stream().anyMatch(e -> e.getAt().matchesGroupAndArtifact(a.getAt()));
    return excluded;
  }

  @Override public String toString() {
    return format("[%s%s]", toExternalForm(),
        exclusions.size() > 0 ? format(" {-%s}", exclusions.size()) : "");
  }

  public static Set<Artifact> artifactsOf(Match xmlDepNode) {
    return xmlDepNode.children().each().stream().map(Artifact::new)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
