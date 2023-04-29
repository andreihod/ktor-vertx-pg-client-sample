package com.andreih

private const val DB_HOST: String = "localhost"
private const val DB_PORT: Int = 5432
private const val DB_DATABASE: String = "todo"
private const val DB_USER: String = "postgres"
private const val DB_PASSWORD: String = "postgres"
private const val HTTP_PORT: Int = 8080


data class Env(
    val dataSource: DataSource = DataSource(),
    val http: Http = Http(),
) {
    data class Http(
        val host: String = System.getenv("SERVER_HOST") ?: "localhost",
        val port: Int = System.getenv("SERVER_PORT")?.toIntOrNull() ?: HTTP_PORT,
    )

    data class DataSource(
        val host: String = System.getenv("DB_HOST") ?: DB_HOST,
        val port: Int = System.getenv("DB_PORT")?.toInt() ?: DB_PORT,
        val database: String = System.getenv("DB_DATABASE") ?: DB_DATABASE,
        val user: String = System.getenv("DB_USER") ?: DB_USER,
        val password: String = System.getenv("DB_PASSWORD") ?: DB_PASSWORD,
    )
}
