package com.salesforce.servicelibs.canteen;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.utils.Os;
import org.apache.maven.shared.utils.io.FileUtils;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;

@Mojo( name = "canteen", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class CanteenMojo extends AbstractMojo {
    private static final String CANTEEN_BOOTSTRAP = "canteen-bootstrap";
    private static final List<String> PLATFORMS = Arrays.asList("osx-x86_64", "linux-x86_64", "windows-x86_64");

    @Component
    protected RepositorySystem repositorySystem;

    @Component
    protected ResolutionErrorHandler resolutionErrorHandler;

    @Component
    private MavenProjectHelper projectHelper;

    @Parameter( defaultValue = "${plugin}", readonly = true ) // Maven 3 only
    protected PluginDescriptor plugin;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    @Parameter(required = true, readonly = true, property = "localRepository")
    protected ArtifactRepository localRepository;

    @Parameter(required = true, readonly = true, defaultValue = "${project.remoteArtifactRepositories}")
    protected List<ArtifactRepository> remoteRepositories;

    @Parameter(defaultValue = "${project.build.directory}", required = true)
    protected File outputDirectory;

    @Parameter(defaultValue = "${project.build.finalName}", readonly = true)
    protected String finalName;

    /**
     * The directory (typically under /target) where Canteen bootstrap binaries are temporarily staged.
     */
    @Parameter(required = true, defaultValue = "${project.build.directory}/" + CANTEEN_BOOTSTRAP)
    protected File canteenBootstrapDirectory;

    /**
     * Classifier to use when locating the artifact to bootstrap. If no classifier is provided, the module default
     * artifact will be used.
     */
    @Parameter(required = false)
    protected String classifier;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        for (String platform : PLATFORMS) {
            // Locate (and maybe download) the bootstrap artifact
            Artifact bootstrapArtifact = createDependencyArtifact(plugin.getGroupId(), CANTEEN_BOOTSTRAP, plugin.getVersion(), "exe", platform);
            File bootstrapFile = resolveBinaryArtifact(bootstrapArtifact);

            // Locate the source artifact
            Artifact sourceArtifact = getSourceArtifact();

            // Build the bootstrap file
            File targetFile = getTargetFile(platform);
            try (OutputStream targetStream = new FileOutputStream(targetFile)) {
                try (InputStream bootstrapStream = new FileInputStream(bootstrapFile)) {
                    ByteStreams.copy(bootstrapStream, targetStream);
                }
                try (InputStream sourceStream = new FileInputStream(sourceArtifact.getFile())) {
                    ByteStreams.copy(sourceStream, targetStream);
                }
                targetStream.flush();
            } catch (IOException ex) {
                throw new MojoFailureException("Failed to write bootstrapped jar", ex);
            }
            if (!Os.isFamily(Os.FAMILY_WINDOWS)) {
                targetFile.setExecutable(true);
            }

            // Attach the new artifact
            projectHelper.attachArtifact(project, "exe", platform, targetFile);
        }
    }

    /**
     * Creates a dependency artifact from a specification in
     * {@code groupId:artifactId:version[:type[:classifier]]} format.
     *
     * @return artifact object instance.
     */
    private Artifact createDependencyArtifact(String groupId, String artifactId, String version, String type, String classifier) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        dependency.setType(type);
        dependency.setClassifier(classifier);
        dependency.setScope(Artifact.SCOPE_RUNTIME);
        return repositorySystem.createDependencyArtifact(dependency);
    }

    /**
     * Downloads a binary artifact and installs it in the canteen bootstrap directory.
     * @param artifact the artifact to download.
     * @return a handle to the downloaded file.
     */
    private File resolveBinaryArtifact(final Artifact artifact) throws MojoExecutionException {
        final ArtifactResolutionResult result;
        try {
            final ArtifactResolutionRequest request = new ArtifactResolutionRequest()
                    .setArtifact(project.getArtifact())
                    .setResolveRoot(false)
                    .setResolveTransitively(false)
                    .setArtifactDependencies(singleton(artifact))
                    .setManagedVersionMap(emptyMap())
                    .setLocalRepository(localRepository)
                    .setRemoteRepositories(remoteRepositories)
                    .setOffline(session.isOffline())
                    .setForceUpdate(session.getRequest().isUpdateSnapshots())
                    .setServers(session.getRequest().getServers())
                    .setMirrors(session.getRequest().getMirrors())
                    .setProxies(session.getRequest().getProxies());

            result = repositorySystem.resolve(request);

            resolutionErrorHandler.throwErrors(request, result);
        } catch (final ArtifactResolutionException e) {
            throw new MojoExecutionException("Unable to resolve artifact: " + e.getMessage(), e);
        }

        final Set<Artifact> artifacts = result.getArtifacts();

        if (artifacts == null || artifacts.isEmpty()) {
            throw new MojoExecutionException("Unable to resolve artifact");
        }

        final Artifact resolvedBinaryArtifact = artifacts.iterator().next();
        getLog().debug("Resolved artifact: " + resolvedBinaryArtifact);

        // Copy the file to the project build directory and make it executable
        final File sourceFile = resolvedBinaryArtifact.getFile();
        final String sourceFileName = sourceFile.getName();
        final String targetFileName;
        if (Os.isFamily(Os.FAMILY_WINDOWS) && !sourceFileName.endsWith(".exe")) {
            targetFileName = sourceFileName + ".exe";
        } else {
            targetFileName = sourceFileName;
        }
        final File targetFile = new File(canteenBootstrapDirectory, targetFileName);
        if (targetFile.exists()) {
            // The file must have already been copied in a prior plugin execution/invocation
            getLog().debug("Executable file already exists: " + targetFile.getAbsolutePath());
            return targetFile;
        }
        try {
            FileUtils.forceMkdir(canteenBootstrapDirectory);
        } catch (final IOException e) {
            throw new MojoExecutionException("Unable to create directory " + canteenBootstrapDirectory, e);
        }
        try {
            FileUtils.copyFile(sourceFile, targetFile);
        } catch (final IOException e) {
            throw new MojoExecutionException("Unable to copy the file to " + canteenBootstrapDirectory, e);
        }

        getLog().debug("Executable file resolved: " + targetFile.getAbsolutePath());
        return targetFile;
    }

    /**
     * Return the source {@link Artifact} to repackage. If a classifier is specified and
     * an artifact with that classifier exists, it is used. Otherwise, the main artifact
     * is used.
     * @return the source artifact to repackage
     */
    private Artifact getSourceArtifact() {
        Artifact sourceArtifact = getArtifact(this.classifier);
        return (sourceArtifact != null) ? sourceArtifact : this.project.getArtifact();
    }

    private Artifact getArtifact(String classifier) {
        if (classifier != null) {
            for (Artifact attachedArtifact : this.project.getAttachedArtifacts()) {
                if (classifier.equals(attachedArtifact.getClassifier()) && attachedArtifact.getFile() != null
                        && attachedArtifact.getFile().isFile()) {
                    return attachedArtifact;
                }
            }
        }
        return null;
    }

    /**
     * @return A file to write the bootstrapped artifact to
     */
    private File getTargetFile(String classifier) {
        if (!this.outputDirectory.exists()) {
            this.outputDirectory.mkdirs();
        }
        return new File(this.outputDirectory,
                this.finalName + "-" + classifier + ".exe");
    }
}
