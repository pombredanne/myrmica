package io.vacco.myrmica.maven.schema;

import org.joox.Match;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static io.vacco.myrmica.maven.schema.Constants.*;
import static org.joox.JOOX.*;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @see <a href="https://maven.apache.org/ref/3.6.0/maven-core/artifact-handlers.html">artifact-handlers</a>
 */
public class Component implements Cloneable {

  public String type;
  public String extension;
  public String packaging;
  public String classifier;
  public String language;
  public boolean addedToClasspath;

  public String toExternalForm() {
    return format("[%s%s%s%s%s%s]", format("t: %s", type),
        extension != null ? format(", ext: %s", extension) : "",
        packaging != null ? format(", pkg: %s", packaging) : "",
        classifier != null ? format(", clf: %s", classifier) : "",
        language != null ? format(", lng: %s", language) : "",
        format(", rt: %s", addedToClasspath)
    );
  }

  public void setClassifier(Match n) {
    String cl = n.child(ComponentTag.classifier.toString()).text();
    this.classifier = cl != null ? cl : this.classifier;
  }

  @Override public boolean equals(Object o) {
    if (o instanceof Component) {
      Component oc = (Component) o;
      boolean st = type.equals(oc.type);
      boolean se = extension.equals(oc.extension);
      boolean sp = packaging.equals(oc.packaging);
      boolean sc = (classifier == null && oc.classifier == null)
          || classifier != null && classifier.equals(oc.classifier);
      return st && se && sp && sc;
    }
    return false;
  }

  private static final Collection<Component> defaults = new ArrayList<>();

  static {
    try {
      URL handlers = Component.class.getResource("/io/vacco/myrmica/maven/artifact-handlers.xml");
      Match xml = $(handlers);
      defaults.addAll(xml.find("configuration").each().stream().map(Component::from).collect(Collectors.toList()));
    } catch (Exception e) {
      throw new IllegalStateException("Cannot resolve default component artifact handlers.");
    }
  }

  private static Component cloneOf(Component c) {
    if (c == null) return null;
    try { return (Component) c.clone(); }
    catch (CloneNotSupportedException e) { throw new IllegalStateException(e); }
  }

  public static Optional<Component> forType(String t) {
    Optional<Component> result = defaults.stream().filter(cmp -> cmp.type.equals(t)).findFirst();
    return result.map(Component::cloneOf);
  }
  public static Optional<Component> forPackaging(String p) {
    Optional<Component> result = defaults.stream().filter(cmp -> cmp.packaging.equals(p)).findFirst();
    return result.map(Component::cloneOf);
  }
  public static Component from(Match src) {
    requireNonNull(src);
    Component c = new Component();
    c.type = requireNonNull(src.child(ComponentTag.type.toString()).text());
    c.extension = src.child(ComponentTag.extension.toString()).text();
    c.packaging = src.child(ComponentTag.packaging.toString()).text();
    c.setClassifier(src);
    c.language = src.child(ComponentTag.language.toString()).text();
    c.addedToClasspath = Boolean.parseBoolean(src.child(ComponentTag.addedToClasspath.toString()).text());
    if (c.extension == null) c.extension = c.type;
    if (c.packaging == null) c.packaging = c.type;
    return c;
  }

  @Override public String toString() { return toExternalForm(); }
}
