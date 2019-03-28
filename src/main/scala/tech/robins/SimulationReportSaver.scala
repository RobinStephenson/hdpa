package tech.robins
import java.nio.file.{Files, Path}

import com.google.gson.GsonBuilder

object SimulationReportSaver {

  private val gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting().create()

  def saveToFile(report: SimulationReport, path: Path, fileName: String): Path = {
    // TODO improve json output (include task exec reports) hide private files
    val jsonReport: String = gson.toJson(report)
    Files.createDirectories(path) // Create any directories that do not yet exist
    val pathAndFileName = path.resolve(fileName)
    Files.write(pathAndFileName, jsonReport.getBytes)
  }
}
