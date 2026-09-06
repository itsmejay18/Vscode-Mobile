package com.customvscode.mobile

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/**
 * Minimal GitHub API client with zero extra deps.
 * Uses HttpURLConnection + org.json (built into Android).
 * Token = OAuth token or PAT (ghp_...).
 */
object GitHubApi {

    private fun conn(urlStr: String, token: String, method: String = "GET"): HttpURLConnection {
        val c = URL(urlStr).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.setRequestProperty("Authorization", "Bearer $token")
        c.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        c.connectTimeout = 15000
        c.readTimeout = 15000
        return c
    }

    private fun read(c: HttpURLConnection): String {
        val stream = if (c.responseCode in 200..299) c.inputStream else c.errorStream
        return stream.bufferedReader().use { it.readText() }.also { c.disconnect() }
    }

    fun listRepos(token: String): List<String> = try {
        val c = conn("https://api.github.com/user/repos?per_page=50&sort=updated", token)
        val body = read(c)
        val arr = org.json.JSONArray(body)
        (0 until arr.length()).map {
            arr.getJSONObject(it).getString("full_name")
        }
    } catch (_: Exception) { emptyList() }

    data class FileContent(val text: String, val sha: String)

    fun getFile(token: String, repo: String, path: String, ref: String = "main"): FileContent? = try {
        val c = conn("https://api.github.com/repos/$repo/contents/$path?ref=$ref", token)
        val body = read(c)
        val obj = JSONObject(body)
        val b64 = obj.getString("content").replace("\n", "")
        val sha = obj.optString("sha", "")
        val bytes = Base64.getDecoder().decode(b64)
        FileContent(String(bytes, Charsets.UTF_8), sha)
    } catch (_: Exception) { null }

    fun putFile(
        token: String,
        repo: String,
        path: String,
        content: String,
        message: String,
        sha: String?,
        branch: String = "main"
    ): Boolean = try {
        val c = conn("https://api.github.com/repos/$repo/contents/$path", token, "PUT")
        c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json")
        val b64 = Base64.getEncoder().encodeToString(content.toByteArray(Charsets.UTF_8))
        val json = JSONObject()
            .put("message", message)
            .put("content", b64)
            .put("branch", branch)
        if (sha != null) json.put("sha", sha)
        c.outputStream.use { it.write(json.toString().toByteArray()) }
        val code = c.responseCode
        read(c)
        code in 200..299
    } catch (_: Exception) { false }
}
