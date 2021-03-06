package net.jangaroo.hudson;

import hudson.maven.MavenAggregatedReport;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.Action;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;
import hudson.tasks.test.TestResult;
import hudson.tasks.test.TestResultProjectAction;

import java.util.List;
import java.util.Map;

/**
 * {@link MavenAggregatedReport} for surefire report.
 *
 * @author Kohsuke Kawaguchi
 */
public class JooTestAggregatedReport extends AggregatedTestResultAction implements MavenAggregatedReport {
  JooTestAggregatedReport(MavenModuleSetBuild owner) {
    super(owner);
  }

  public void update(Map<MavenModule, List<MavenBuild>> moduleBuilds, MavenBuild newBuild) {
    super.update(((MavenModuleSetBuild) owner).findModuleBuildActions(JooTestReport.class));
  }

  public Class<JooTestReport> getIndividualActionType() {
    return JooTestReport.class;
  }

  public Action getProjectAction(MavenModuleSet moduleSet) {
    return new TestResultProjectAction(moduleSet);
  }

  @Override
  protected String getChildName(AbstractTestResultAction tr) {
    return ((MavenModule) tr.owner.getProject()).getModuleName().toString();
  }

  @Override
  public MavenBuild resolveChild(Child child) {
    MavenModuleSet mms = (MavenModuleSet) owner.getProject();
    MavenModule m = mms.getModule(child.name);
    if (m != null) {
      return m.getBuildByNumber(child.build);
    }
    return null;
  }

  public JooTestReport getChildReport(Child child) {
    MavenBuild b = resolveChild(child);
    if (b == null) {
      return null;
    }
    return b.getAction(JooTestReport.class);
  }

  /**
   *
   */
  @Override
  public String getTestResultPath(TestResult it) {
    StringBuilder path = new StringBuilder("../");
    path.append(it.getOwner().getProject().getShortUrl());
    path.append(it.getOwner().getNumber());
    path.append("/");
    path.append(getUrlName());
    path.append("/");
    path.append(it.getRelativePathFrom(null));
    return path.toString();
  }
}
