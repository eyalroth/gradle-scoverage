package org.scoverage

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
// don't use scala.collection.JavaConversions as it doesn't exist in Scala 2.13
import scala.collection.JavaConverters
import scala.collection.Seq
import scala.collection.Set
import scoverage.Coverage
import scoverage.IOUtils
import scoverage.Serializer

@CacheableTask
class ScoverageReport extends DefaultTask {

    ScoverageRunner runner

    @Input
    final Property<File> dataDir = project.objects.property(File)

    @Input
    final Property<File> sources = project.objects.property(File)

    @OutputDirectory
    final Property<File> reportDir = project.objects.property(File)

    @Input
    final Property<Boolean> coverageOutputCobertura = project.objects.property(Boolean)
    @Input
    final Property<Boolean> coverageOutputXML = project.objects.property(Boolean)
    @Input
    final Property<Boolean> coverageOutputHTML = project.objects.property(Boolean)
    @Input
    final Property<Boolean> coverageDebug = project.objects.property(Boolean)

    @TaskAction
    def report() {
        runner.run {
            reportDir.get().mkdirs()

            File coverageFile = Serializer.coverageFile(dataDir.get())

            if (!coverageFile.exists()) {
                project.logger.info("[scoverage] Could not find coverage file, skipping...")
            } else {
                File[] array = IOUtils.findMeasurementFiles(dataDir.get())
                Seq<File> measurementFiles = JavaConverters.asScalaBuffer(Arrays.asList(array)).toSeq()

                Coverage coverage = Serializer.deserialize(coverageFile)

                Set<Object> measurements = IOUtils.invoked(measurementFiles)
                coverage.apply(measurements)

                new ScoverageWriter(project.logger).write(
                        sources.get(),
                        reportDir.get(),
                        coverage,
                        coverageOutputCobertura.get(),
                        coverageOutputXML.get(),
                        coverageOutputHTML.get(),
                        coverageDebug.get())
            }
        }
    }
}
