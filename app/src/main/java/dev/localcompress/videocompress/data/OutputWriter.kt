package dev.localcompress.videocompress.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

object OutputWriter {

    /** Copies [sourceFile] into the SAF tree at [treeUri] as [displayName], returning the new document's Uri. */
    fun writeToOutputFolder(context: Context, treeUri: Uri, sourceFile: File, displayName: String): Uri {
        val treeDoc = DocumentFile.fromTreeUri(context, treeUri)
            ?: error("Папка сохранения недоступна, выберите её заново в настройках")
        if (!treeDoc.canWrite()) error("Нет прав на запись в выбранную папку")

        treeDoc.findFile(displayName)?.delete()
        val newDoc = treeDoc.createFile("video/mp4", displayName)
            ?: error("Не удалось создать файл в выбранной папке")

        context.contentResolver.openOutputStream(newDoc.uri)?.use { out ->
            sourceFile.inputStream().use { input -> input.copyTo(out) }
        } ?: error("Не удалось открыть поток записи для сохранения файла")

        return newDoc.uri
    }

    fun folderDisplayName(context: Context, treeUri: Uri): String =
        DocumentFile.fromTreeUri(context, treeUri)?.name ?: treeUri.lastPathSegment ?: treeUri.toString()
}
