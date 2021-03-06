package unit;

import io.vacco.myrmica.maven.impl.Repository;
import io.vacco.myrmica.maven.impl.ResolutionResult;
import io.vacco.myrmica.maven.schema.Artifact;
import io.vacco.myrmica.maven.schema.Coordinates;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.Assert.*;

public class ResolutionStats {

  public final Coordinates coordinates;
  public final Set<Coordinates> hit = new TreeSet<>();
  public final Set<Coordinates> miss = new TreeSet<>();
  public final Set<Coordinates> slack = new TreeSet<>();
  public ResolutionResult resolutionResult;

  public ResolutionStats(Coordinates c) {
    this.coordinates = Objects.requireNonNull(c);
  }

  public static Set<Coordinates> loadRef(String classPathLocation) throws IOException {
    String [] lines = new Scanner(
        MyrmicaSpec.class.getResource(classPathLocation).openStream(), "UTF-8"
    ).useDelimiter("\\A").next().split("\n");
    return Arrays.stream(lines)
        .map(l0 -> l0.split(":"))
        .map(l0 -> Coordinates.from(l0[0], l0[1],
            l0[2].contains("->") ? l0[2].split("->")[1] : l0[2])
        ).collect(Collectors.toCollection(TreeSet::new));
  }

  public static ResolutionStats installAndMatch(Repository repo, Coordinates target, String gradleRef) throws IOException {

    Set<Coordinates> grdRef = ResolutionStats.loadRef(gradleRef);
    ResolutionStats rs = new ResolutionStats(target);
    ResolutionResult rr = repo.loadRuntimeArtifactsAt(target);
    Map<Artifact, Path> binaries = repo.installLoadedArtifacts(rr);

    assertFalse(binaries.isEmpty());
    rs.resolutionResult = rr;

    grdRef.forEach(refCoord -> {
      Optional<Coordinates> hit = binaries.keySet().stream()
          .filter(a -> a.at.equals(refCoord))
          .map(a -> a.at).findFirst();
      if (hit.isPresent()) { rs.hit.add(refCoord); }
      else { rs.miss.add(refCoord); }
    });
    binaries.keySet().forEach(a -> {
      if (!grdRef.contains(a.at)) {
        rs.slack.add(a.at);
      }
    });

    return rs;
  }

  @Override public String toString() {
    return String.format("Hit: %03d, Miss: %03d, Slack: %03d -> %s",
        hit.size(), miss.size(), slack.size(), coordinates);
  }
}
