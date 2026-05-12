package alienkrishn.hbctool

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import alienkrishn.hbctool.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.ZipInputStream

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var selectedFileUri: Uri? = null
    private var selectedFilePath: String? = null
    private var lastOutputDir: String? = null          // Stores the last output directory
    private var isDisassembledOutput: Boolean = false   // Flag indicating whether output is from disassembly
    
    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedFile(uri)
            }
        }
    }
    
    // Export ZIP file picker
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                exportToUri(uri)
            }
        }
    }
    
    // Export Bundle file picker
    private val exportBundleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                exportBundleToUri(uri)
            }
        }
    }
    
    // Permission request launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            openFilePicker()
        } else {
            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        
        // Enable edge-to-edge display, extend content under status and navigation bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Set status/navigation bar icon colors based on current theme
        val isDarkMode = (resources.configuration.uiMode and 
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.statusBarColor = getColor(android.R.color.transparent)
            // Light mode -> dark icons, dark mode -> light icons
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !isDarkMode
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = getColor(android.R.color.transparent)
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = !isDarkMode
        }
        
        // Initialize Python
        initPython()
        
        // Set up click listeners
        setupListeners()
        
        // Check for existing files in input/output folders
        checkExistingFiles()
        
        // Handle file opened from external intent
        handleIntent(intent)
    }
    
    private fun checkExistingFiles() {
        // Check for input file
        val inputDir = File(filesDir, "input")
        if (inputDir.exists()) {
            val inputFiles = inputDir.listFiles()
            if (inputFiles != null && inputFiles.isNotEmpty()) {
                val latestFile = inputFiles.maxByOrNull { it.lastModified() }
                if (latestFile != null && latestFile.exists()) {
                    selectedFilePath = latestFile.absolutePath
                    binding.filePathText.text = "Selected: ${latestFile.name}\nPath: ${latestFile.absolutePath}"
                    
                    // Enable buttons according to file type
                    when {
                        latestFile.name.endsWith(".bundle", ignoreCase = true) -> {
                            binding.disassembleButton.isEnabled = true
                            binding.cleanEnvironmentButton.isEnabled = true
                        }
                        latestFile.name.endsWith(".hasm", ignoreCase = true) -> {
                            binding.assembleButton.isEnabled = true
                            binding.cleanEnvironmentButton.isEnabled = true
                        }
                    }
                }
            }
        }
        
        // Check for output directory
        val outputDir = File(filesDir, "output")
        if (outputDir.exists()) {
            val outputDirs = outputDir.listFiles { file -> file.isDirectory }
            if (outputDirs != null && outputDirs.isNotEmpty()) {
                val latestOutputDir = outputDirs.maxByOrNull { it.lastModified() }
                if (latestOutputDir != null && latestOutputDir.exists()) {
                    lastOutputDir = latestOutputDir.absolutePath
                    binding.cleanEnvironmentButton.isEnabled = true
                    // Do not enable export buttons on start – wait for user action
                }
            }
        }
    }
    
    private fun checkAndLoadFileFromOutput(outputDir: File) {
        val hasmFiles = outputDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("hasm", ignoreCase = true) }
            .toList()
        
        // Auto‑load only .hasm files from disassembly (not .bundle from assembly)
        if (hasmFiles.size == 1) {
            val hasmFile = hasmFiles.first()
            // Assembly needs the whole directory (metadata.json, string.json, instruction.hasm)
            hasmFile.parentFile?.let { parent ->
                selectedFilePath = parent.absolutePath
                binding.filePathText.text = "Selected directory: ${parent.name}\n(Assembly requires full directory)"
                binding.assembleButton.isEnabled = true
                binding.disassembleButton.isEnabled = false
                binding.cleanEnvironmentButton.isEnabled = true
            }
        }
    }
    
    private fun initPython() {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
            appendLog("✓ Python engine started")
        }
    }
    
    private fun setupListeners() {
        // Select file button
        binding.selectFileButton.setOnClickListener {
            checkPermissionsAndPickFile()
        }
        
        // Disassemble button
        binding.disassembleButton.setOnClickListener {
            selectedFilePath?.let { path ->
                performDisassemble(path)
            }
        }
        
        // Assemble button
        binding.assembleButton.setOnClickListener {
            selectedFilePath?.let { path ->
                performAssemble(path)
            }
        }
        
        // Clear log button
        binding.clearLogButton.setOnClickListener {
            binding.logTextView.text = "Log cleared\n"
        }
        
        // Copy log button
        binding.copyLogButton.setOnClickListener {
            copyLogToClipboard()
        }
        
        // Clean environment button
        binding.cleanEnvironmentButton.setOnClickListener {
            cleanEnvironment()
        }
        
        // Export ZIP button
        binding.exportButton.setOnClickListener {
            exportOutputFiles()
        }
        
        // Export Bundle button
        binding.exportBundleButton.setOnClickListener {
            exportBundleFile()
        }
    }
    
    private fun cleanEnvironment() {
        val inputDir = File(filesDir, "input")
        val outputDir = File(filesDir, "output")
        
        val hasInputFiles = inputDir.exists() && inputDir.listFiles()?.isNotEmpty() == true
        val hasOutputFiles = outputDir.exists() && outputDir.listFiles()?.isNotEmpty() == true
        
        if (!hasInputFiles && !hasOutputFiles) {
            Toast.makeText(this, "No files to clean", Toast.LENGTH_SHORT).show()
            return
        }
        
        val message = buildString {
            append("Are you sure you want to clean the environment? This will delete:\n")
            if (hasInputFiles) {
                val fileCount = inputDir.listFiles()?.size ?: 0
                append("\n• Input folder: $fileCount file(s)")
            }
            if (hasOutputFiles) {
                val dirCount = outputDir.listFiles()?.size ?: 0
                append("\n• Output folder: $dirCount subfolder(s)")
            }
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Clean Environment")
            .setMessage(message)
            .setPositiveButton("Clean") { _, _ ->
                var successCount = 0
                var failCount = 0
                
                // Clean input folder
                if (hasInputFiles) {
                    try {
                        var deletedCount = 0
                        inputDir.listFiles()?.forEach { file ->
                            if (file.deleteRecursively()) {
                                deletedCount++
                            }
                        }
                        if (deletedCount > 0) {
                            appendLog("✓ Cleaned Input folder: deleted $deletedCount file(s)")
                            successCount++
                        }
                    } catch (e: Exception) {
                        appendLog("❌ Failed to clean Input folder: ${e.message}")
                        failCount++
                    }
                }
                
                // Clean output folder
                if (hasOutputFiles) {
                    try {
                        var deletedCount = 0
                        outputDir.listFiles()?.forEach { dir ->
                            if (dir.deleteRecursively()) {
                                deletedCount++
                            }
                        }
                        if (deletedCount > 0) {
                            appendLog("✓ Cleaned Output folder: deleted $deletedCount subfolder(s)")
                            successCount++
                        }
                    } catch (e: Exception) {
                        appendLog("❌ Failed to clean Output folder: ${e.message}")
                        failCount++
                    }
                }
                
                // Reset all state and UI
                selectedFilePath = null
                selectedFileUri = null
                lastOutputDir = null
                isDisassembledOutput = false
                binding.filePathText.text = getString(R.string.select_file_hint)
                binding.disassembleButton.isEnabled = false
                binding.assembleButton.isEnabled = false
                binding.cleanEnvironmentButton.isEnabled = false
                binding.exportButton.isEnabled = false
                binding.exportBundleButton.isEnabled = false
                
                // Show result
                val resultMessage = when {
                    failCount == 0 -> "Clean complete"
                    successCount == 0 -> "Clean failed"
                    else -> "Partial clean"
                }
                Toast.makeText(this, resultMessage, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun copyLogToClipboard() {
        val logText = binding.logTextView.text.toString()
        if (logText.isBlank() || logText == "Log cleared\n") {
            Toast.makeText(this, "Log is empty", Toast.LENGTH_SHORT).show()
            return
        }
        
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("HBC-Tool Log", logText)
        clipboard.setPrimaryClip(clip)
        
        Toast.makeText(this, "Log copied to clipboard", Toast.LENGTH_SHORT).show()
    }
    
    private fun checkPermissionsAndPickFile() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ does not need storage permission
                openFilePicker()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11-12
                openFilePicker()
            }
            else -> {
                // Android 10 and below
                val permissions = arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                
                val hasPermissions = permissions.all {
                    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
                }
                
                if (hasPermissions) {
                    openFilePicker()
                } else {
                    permissionLauncher.launch(permissions)
                }
            }
        }
    }
    
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            // Support .bundle, .hasm and .zip files
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/octet-stream",
                "text/plain",
                "application/zip"
            ))
        }
        filePickerLauncher.launch(intent)
    }
    
    private fun handleSelectedFile(uri: Uri) {
        try {
            selectedFileUri = uri
            
            // Get file name
            val fileName = getFileName(uri) ?: "Unknown file"
            
            // Check if it's a ZIP file
            if (fileName.endsWith(".zip", ignoreCase = true)) {
                // Handle ZIP import
                importFromUri(uri)
                return
            }
            
            // Handle normal files (.bundle or .hasm)
            val inputFile = File(filesDir, "input/$fileName")
            inputFile.parentFile?.mkdirs()
            
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(inputFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            selectedFilePath = inputFile.absolutePath
            
            // Update UI
            binding.filePathText.text = "Selected: $fileName\nPath: ${inputFile.absolutePath}"
            
            // Enable appropriate buttons based on file type
            when {
                fileName.endsWith(".bundle", ignoreCase = true) -> {
                    binding.disassembleButton.isEnabled = true
                    binding.assembleButton.isEnabled = false
                    binding.cleanEnvironmentButton.isEnabled = true
                    appendLog("✓ Loaded Bundle file: $fileName")
                }
                fileName.endsWith(".hasm", ignoreCase = true) -> {
                    // A single .hasm file cannot be assembled; assembly requires a full directory
                    binding.disassembleButton.isEnabled = false
                    binding.assembleButton.isEnabled = false
                    binding.cleanEnvironmentButton.isEnabled = true
                    appendLog("⚠ A single HASM file cannot be assembled")
                    appendLog("Hint: Assembly requires a complete directory containing:")
                    appendLog("  - metadata.json")
                    appendLog("  - string.json")
                    appendLog("  - instruction.hasm")
                    appendLog("Suggestion: Pack these files into a ZIP and re-import")
                    Toast.makeText(this, "HASM file requires a full directory to assemble", Toast.LENGTH_LONG).show()
                }
                else -> {
                    binding.disassembleButton.isEnabled = false
                    binding.assembleButton.isEnabled = false
                    binding.cleanEnvironmentButton.isEnabled = true
                    appendLog("⚠ Unrecognized file type")
                }
            }
            
        } catch (e: Exception) {
            appendLog("❌ Failed to load file: ${e.message}")
            Toast.makeText(this, "File load failed", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun performDisassemble(inputPath: String) {
        lifecycleScope.launch {
            try {
                showProgress(true)
                binding.disassembleButton.isEnabled = false
                
                appendLog("\n========== Starting Disassembly ==========")
                appendLog("Input file: $inputPath")
                
                // Prepare output directory
                val outputDir = File(filesDir, "output/hasm_${System.currentTimeMillis()}")
                outputDir.mkdirs()
                appendLog("Output directory: ${outputDir.absolutePath}")
                
                val result = withContext(Dispatchers.IO) {
                    try {
                        // Call Python
                        val python = Python.getInstance()
                        val bridge = python.getModule("api_bridge")
                        
                        val pyResult = bridge.callAttr("do_disassemble", inputPath, outputDir.absolutePath)
                        
                        pyResult
                    } catch (e: Exception) {
                        appendLog("❌ Python call exception: ${e.javaClass.simpleName}")
                        appendLog("Exception message: ${e.message}")
                        e.printStackTrace()
                        throw e
                    }
                }
                
                // Parse result
                try {
                    // Use Python dict's get method to access keys
                    val status = result.callAttr("get", "status")?.toString() ?: "unknown"
                    
                    val log = result.callAttr("get", "log")?.toString() ?: ""
                    val error = result.callAttr("get", "error")?.toString()
                
                    if (status == "success") {
                        appendLog("✓ Disassembly complete!")
                        // Display Python log (contains version, file size, etc.)
                        if (log.isNotEmpty()) {
                            appendLog(log)
                        }
                        // Record output directory and enable delete/export buttons
                        lastOutputDir = outputDir.absolutePath
                        isDisassembledOutput = true   // Mark as disassembly output
                        binding.cleanEnvironmentButton.isEnabled = true
                        binding.exportButton.isEnabled = true          // Enable ZIP export
                        binding.exportBundleButton.isEnabled = false   // Disable Bundle export
                        
                        Toast.makeText(this@MainActivity, R.string.operation_success, Toast.LENGTH_SHORT).show()
                    } else {
                        appendLog("❌ Disassembly failed")
                        if (log.isNotEmpty()) {
                            appendLog(log)
                        }
                        if (error != null) {
                            appendLog("Error details: $error")
                        }
                        Toast.makeText(this@MainActivity, R.string.operation_failed, Toast.LENGTH_SHORT).show()
                    }
                } catch (parseException: Exception) {
                    appendLog("❌ Failed to parse return value: ${parseException.message}")
                    appendLog("Raw return value: $result")
                    Toast.makeText(this@MainActivity, "Failed to parse return value", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                appendLog("❌ Error: ${e.message}\n${e.stackTraceToString()}")
                Toast.makeText(this@MainActivity, "Operation failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                showProgress(false)
                binding.disassembleButton.isEnabled = true
            }
        }
    }
    
    private fun performAssemble(inputPath: String) {
        lifecycleScope.launch {
            try {
                showProgress(true)
                binding.assembleButton.isEnabled = false
                
                appendLog("\n========== Starting Assembly ==========")
                appendLog("Input file: $inputPath")
                
                // Prepare output directory
                val outputDir = File(filesDir, "output/bundle_${System.currentTimeMillis()}")
                outputDir.mkdirs()
                appendLog("Output directory: ${outputDir.absolutePath}")
                
                val result = withContext(Dispatchers.IO) {
                    try {
                        val python = Python.getInstance()
                        val bridge = python.getModule("api_bridge")
                        
                        val pyResult = bridge.callAttr("do_assemble", inputPath, outputDir.absolutePath)
                        
                        pyResult
                    } catch (e: Exception) {
                        appendLog("❌ Python call exception: ${e.javaClass.simpleName}")
                        appendLog("Exception message: ${e.message}")
                        e.printStackTrace()
                        throw e
                    }
                }
                
                // Parse result using Python dict's get method
                val status = result.callAttr("get", "status")?.toString() ?: "unknown"
                val log = result.callAttr("get", "log")?.toString() ?: ""
                val error = result.callAttr("get", "error")?.toString()
                
                if (status == "success") {
                    appendLog("✓ Assembly complete!")
                    if (log.isNotEmpty()) {
                        appendLog(log)
                    }
                    // Record output directory and enable buttons
                    lastOutputDir = outputDir.absolutePath
                    isDisassembledOutput = false   // Mark as assembly output
                    binding.cleanEnvironmentButton.isEnabled = true
                    binding.exportButton.isEnabled = false         // Disable ZIP export
                    binding.exportBundleButton.isEnabled = true    // Enable Bundle export
                    
                    Toast.makeText(this@MainActivity, R.string.operation_success, Toast.LENGTH_SHORT).show()
                } else {
                    appendLog("❌ Assembly failed")
                    if (log.isNotEmpty()) {
                        appendLog(log)
                    }
                    if (error != null) {
                        appendLog("Error details: $error")
                    }
                    Toast.makeText(this@MainActivity, R.string.operation_failed, Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                appendLog("❌ Error: ${e.message}\n${e.stackTraceToString()}")
                Toast.makeText(this@MainActivity, "Operation failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                showProgress(false)
                binding.assembleButton.isEnabled = true
            }
        }
    }
    
    private fun showProgress(show: Boolean) {
        binding.progressBar.isVisible = show
    }
    
    private fun appendLog(message: String) {
        runOnUiThread {
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            binding.logTextView.append("[$timestamp] $message\n")
        }
    }
    
    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            }
        }
        return result
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun exportOutputFiles() {
        val outputPath = lastOutputDir
        if (outputPath == null) {
            Toast.makeText(this, "No files to export", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Verify this is disassembly output
        if (!isDisassembledOutput) {
            Toast.makeText(this, "ZIP export is only available for disassembly output", Toast.LENGTH_LONG).show()
            return
        }
        
        val outputDir = File(outputPath)
        if (!outputDir.exists() || !outputDir.isDirectory) {
            Toast.makeText(this, "Output directory does not exist", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Verify required disassembly files exist
        val hasMetadata = File(outputDir, "metadata.json").exists()
        val hasString = File(outputDir, "string.json").exists()
        val hasHasm = outputDir.walkTopDown().any { it.extension.equals("hasm", ignoreCase = true) }
        
        if (!hasMetadata || !hasString || !hasHasm) {
            appendLog("❌ Export failed: missing required disassembly files")
            appendLog("Required: metadata.json, string.json, instruction.hasm")
            Toast.makeText(this, "Output directory does not contain complete disassembly files", Toast.LENGTH_LONG).show()
            return
        }
        
        // Create timestamped filename
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val fileName = "hbctool_output_$timestamp.zip"
        
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        exportLauncher.launch(intent)
    }
    
    private fun exportToUri(uri: Uri) {
        val outputPath = lastOutputDir ?: return
        val outputDir = File(outputPath)
        
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipStream ->
                    outputDir.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val relativePath = file.relativeTo(outputDir).path
                            val zipEntry = ZipEntry(relativePath)
                            zipStream.putNextEntry(zipEntry)
                            
                            FileInputStream(file).use { inputStream ->
                                inputStream.copyTo(zipStream)
                            }
                            zipStream.closeEntry()
                        }
                    }
                }
            }
            
            runOnUiThread {
                appendLog("✓ File exported")
                Toast.makeText(this, "Export successful", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            runOnUiThread {
                appendLog("❌ Export failed: ${e.message}")
                Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun exportBundleFile() {
        val outputPath = lastOutputDir
        if (outputPath == null) {
            Toast.makeText(this, "No Bundle file to export", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Verify this is assembly output
        if (isDisassembledOutput) {
            Toast.makeText(this, "Bundle export is only available for assembly output", Toast.LENGTH_LONG).show()
            return
        }
        
        val outputDir = File(outputPath)
        if (!outputDir.exists() || !outputDir.isDirectory) {
            Toast.makeText(this, "Output directory does not exist", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Find .bundle file
        val bundleFiles = outputDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("bundle", ignoreCase = true) }
            .toList()
        
        if (bundleFiles.isEmpty()) {
            appendLog("❌ Export failed: Bundle file not found")
            Toast.makeText(this, "Output directory does not contain a Bundle file", Toast.LENGTH_LONG).show()
            return
        }
        
        if (bundleFiles.size > 1) {
            appendLog("❌ Export failed: Multiple Bundle files found")
            Toast.makeText(this, "Output directory contains multiple Bundle files", Toast.LENGTH_LONG).show()
            return
        }
        
        // Create filename, ensure .bundle extension
        val bundleFile = bundleFiles.first()
        val fileName = bundleFile.name.let { name ->
            if (name.endsWith(".bundle", ignoreCase = true)) {
                name
            } else {
                "$name.bundle"
            }
        }
        
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"  // Use wildcard MIME type to avoid system suggesting .bin
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        exportBundleLauncher.launch(intent)
    }
    
    private fun exportBundleToUri(uri: Uri) {
        val outputPath = lastOutputDir ?: return
        val outputDir = File(outputPath)
        
        try {
            // Find .bundle file
            val bundleFiles = outputDir.walkTopDown()
                .filter { it.isFile && it.extension.equals("bundle", ignoreCase = true) }
                .toList()
            
            if (bundleFiles.isEmpty()) {
                runOnUiThread {
                    appendLog("❌ Export failed: Bundle file not found")
                    Toast.makeText(this, "Bundle file not found", Toast.LENGTH_SHORT).show()
                }
                return
            }
            
            val bundleFile = bundleFiles.first()
            
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(bundleFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            runOnUiThread {
                appendLog("✓ Bundle file exported: ${bundleFile.name}")
                Toast.makeText(this, "Export successful", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            runOnUiThread {
                appendLog("❌ Export failed: ${e.message}")
                Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun importFromUri(uri: Uri) {
        try {
            appendLog("\n========== Starting Import ==========\nImport file: ${getFileName(uri) ?: "Unknown"}")
            
            // Create new output directory
            val outputDir = File(filesDir, "output/imported_${System.currentTimeMillis()}")
            outputDir.mkdirs()
            
            var fileCount = 0
            contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipStream ->
                    var entry = zipStream.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val file = File(outputDir, entry.name)
                            file.parentFile?.mkdirs()
                            
                            FileOutputStream(file).use { outputStream ->
                                zipStream.copyTo(outputStream)
                            }
                            fileCount++
                        }
                        zipStream.closeEntry()
                        entry = zipStream.nextEntry
                    }
                }
            }
            
            if (fileCount > 0) {
                lastOutputDir = outputDir.absolutePath
                isDisassembledOutput = true   // Imported ZIP is assumed to be disassembly output
                binding.cleanEnvironmentButton.isEnabled = true
                binding.exportButton.isEnabled = true
                binding.exportBundleButton.isEnabled = false
                
                appendLog("✓ Import complete!")
                appendLog("Extracted files: $fileCount")
                appendLog("Output directory: ${outputDir.name}")
                
                // Check for processable files (.hasm or .bundle)
                val hasmFiles = outputDir.walkTopDown()
                    .filter { it.isFile && it.extension.equals("hasm", ignoreCase = true) }
                    .toList()
                val bundleFiles = outputDir.walkTopDown()
                    .filter { it.isFile && it.extension.equals("bundle", ignoreCase = true) }
                    .toList()
                
                when {
                    hasmFiles.size == 1 && bundleFiles.isEmpty() -> {
                        // Exactly one .hasm file → auto‑load for assembly (needs directory path)
                        val hasmFile = hasmFiles.first()
                        hasmFile.parentFile?.let { parent ->
                            selectedFilePath = parent.absolutePath
                            binding.filePathText.text = "Selected directory: ${parent.name}\n(Assembly requires full directory)"
                            binding.assembleButton.isEnabled = true
                            binding.disassembleButton.isEnabled = false
                            binding.cleanEnvironmentButton.isEnabled = true
                            appendLog("✓ Auto‑loaded HASM directory: ${parent.name}")
                            Toast.makeText(this, "Import successful, loaded ${parent.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    bundleFiles.size == 1 && hasmFiles.isEmpty() -> {
                        // Exactly one .bundle file → auto‑load for disassembly
                        val bundleFile = bundleFiles.first()
                        selectedFilePath = bundleFile.absolutePath
                        binding.filePathText.text = "Selected: ${bundleFile.name}\nPath: ${bundleFile.absolutePath}"
                        binding.disassembleButton.isEnabled = true
                        binding.assembleButton.isEnabled = false
                        binding.cleanEnvironmentButton.isEnabled = true
                        appendLog("✓ Auto‑loaded Bundle file: ${bundleFile.name}")
                        Toast.makeText(this, "Import successful, loaded ${bundleFile.name}", Toast.LENGTH_SHORT).show()
                    }
                    hasmFiles.isNotEmpty() || bundleFiles.isNotEmpty() -> {
                        // Multiple processable files
                        val totalFiles = hasmFiles.size + bundleFiles.size
                        appendLog("⚠ Found $totalFiles processable file(s) (${hasmFiles.size} HASM, ${bundleFiles.size} Bundle)")
                        appendLog("Hint: Use 'Select File' button to manually choose from the imported directory")
                        Toast.makeText(this, "Import successful, total $fileCount files", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        // No processable files
                        appendLog("⚠ No processable files found (.hasm or .bundle)")
                        Toast.makeText(this, "Import successful, total $fileCount files", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                outputDir.deleteRecursively()
                appendLog("❌ ZIP file is empty")
                Toast.makeText(this, "ZIP file is empty", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            appendLog("❌ Import failed: ${e.message}")
            Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.about)
            .setMessage(R.string.about_info)
            .setPositiveButton("OK", null)
            .setNeutralButton("GitHub") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://github.com/Anon4You/HBCToolApp")
                }
                startActivity(intent)
            }
            .show()
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }
    
    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                handleSelectedFile(uri)
            }
        }
    }
}