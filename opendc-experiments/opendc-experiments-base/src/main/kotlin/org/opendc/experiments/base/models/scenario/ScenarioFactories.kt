/*
 * Copyright (c) 2024 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.opendc.experiments.base.models.scenario

import AllocationPolicySpec
import TopologySpec
import WorkloadSpec
import org.opendc.compute.simulator.failure.getFailureModel
import org.opendc.compute.topology.TopologyReader
import org.opendc.compute.topology.clusterTopology
import org.opendc.compute.topology.specs.TopologyJSONSpec
import org.opendc.experiments.base.models.scenario.specs.ScenarioSpec
import java.io.File

private val scenarioReader = ScenarioReader()
private val scenarioWriter = ScenarioWriter()

/**
 * Returns a list of Scenarios from a given file path (input).
 *
 * @param filePath The path to the file containing the scenario specifications.
 * @return A list of Scenarios.
 */
public fun getScenarios(filePath: String): List<Scenario> {
    return getScenarios(File(filePath))
}

/**
 * Returns a list of Scenarios from a given file. Reads and decodes the contents of the (JSON) file.
 *
 * @param file The file containing the scenario specifications.
 * @return A list of Scenarios.
 */
public fun getScenarios(file: File): List<Scenario> {
    return getScenarios(scenarioReader.read(file))
}

/**
 * Returns a list of Scenarios from a given ScenarioSpec.
 *
 * @param scenarioSpec The ScenarioSpec containing the scenario specifications.
 * @return A list of Scenarios.
 */
public fun getScenarios(scenarioSpec: ScenarioSpec): List<Scenario> {
    return getScenarioCombinations(scenarioSpec)
}

/**
 * Returns a list of Scenarios from a given ScenarioSpec by generating all possible combinations of
 * workloads, allocation policies, failure models, and export models within a topology.
 *
 * @param scenarioSpec The ScenarioSpec containing the scenario specifications.
 * @return A list of Scenarios.
 */
public fun getScenarioCombinations(scenarioSpec: ScenarioSpec): List<Scenario> {
    val outputFolder = scenarioSpec.outputFolder + "/" + scenarioSpec.name
    File(outputFolder).mkdirs()

    val trackrPath = outputFolder + "/trackr.json"
    File(trackrPath).createNewFile()

    val topologiesSpec = scenarioSpec.topologies
    val workloads = scenarioSpec.workloads
    val allocationPolicies = scenarioSpec.allocationPolicies
    val failureModels = scenarioSpec.failureModels
    val exportModels = scenarioSpec.exportModels
    val scenarios = mutableListOf<Scenario>()
    var scenarioID = 0

    for (topology in topologiesSpec) {
        for (workload in workloads) {
            for (allocationPolicy in allocationPolicies) {
                for (failureModel in failureModels) {
                    for (carbonTracePath in scenarioSpec.carbonTracePaths) {
                        for (exportModel in exportModels) {
                            val scenario =
                                Scenario(
                                    topology = clusterTopology(File(topology.pathToFile)),
                                    workload = workload,
                                    allocationPolicy = allocationPolicy,
                                    failureModel = getFailureModel(failureModel.failureInterval),
                                    carbonTracePath = carbonTracePath,
                                    exportModel = exportModel,
                                    outputFolder = outputFolder,
                                    name = scenarioID.toString(),
                                    runs = scenarioSpec.runs,
                                    initialSeed = scenarioSpec.initialSeed,
                                )
                            trackScenario(scenarioSpec, outputFolder, scenario, topology)
                            scenarios.add(scenario)
                            scenarioID++
                        }
                    }
                }
            }
        }
    }

    return scenarios
}

/**
 * Returns a list of TopologyJSONSpec from a given list of TopologySpec.
 *
 * @param topologies The list of TopologySpec.
 * @return A list of TopologyJSONSpec.
 */
public fun getTopologies(topologies: List<TopologySpec>): List<TopologyJSONSpec> {
    val readTopologies = mutableListOf<TopologyJSONSpec>()
    for (topology in topologies) {
        readTopologies.add(TopologyReader().read(File(topology.pathToFile)))
    }

    return readTopologies
}

/**
 * Returns a string representing the output folder name for a given ScenarioSpec, CpuPowerModel, AllocationPolicySpec, and topology path.
 *
 * @param scenarioSpec The ScenarioSpec.
 * @param powerModel The CpuPowerModel.
 * @param allocationPolicy The AllocationPolicySpec.
 * @param topologyPath The path to the topology file.
 * @return A string representing the output folder name.
 */
public fun getOutputFolderName(
    scenarioSpec: ScenarioSpec,
    topology: TopologySpec,
    workload: WorkloadSpec,
    allocationPolicy: AllocationPolicySpec,
): String {
    return "scenario=${scenarioSpec.name}" +
        "-topology=${topology.name}" +
        "-workload=${workload.name}" +
        "-scheduler=${allocationPolicy.name}"
}

/**
 * Writes a ScenarioSpec to a file.
 *
 * @param scenarioSpec The ScenarioSpec.
 * @param outputFolder The output folder path.
 * @param scenario The Scenario.
 * @param topologySpec The TopologySpec.

 */
public fun trackScenario(
    scenarioSpec: ScenarioSpec,
    outputFolder: String,
    scenario: Scenario,
    topologySpec: TopologySpec,
) {
    val trackrPath = outputFolder + "/trackr.json"
    scenarioWriter.write(
        ScenarioSpec(
            id = scenarioSpec.id,
            name = scenarioSpec.name,
            topologies = listOf(topologySpec),
            workloads = listOf(scenario.workload),
            allocationPolicies = listOf(scenario.allocationPolicy),
            // when implemented, add failure models here
            carbonTracePaths = listOf(scenario.carbonTracePath),
            exportModels = listOf(scenario.exportModel),
            outputFolder = scenario.outputFolder,
            initialSeed = scenario.initialSeed,
            runs = scenario.runs,
        ),
        File(trackrPath),
    )

    // remove the last comma
    File(trackrPath).writeText(File(trackrPath).readText().dropLast(3) + "]")
}
