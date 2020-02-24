package auth.vixen

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

@SpringBootApplication
@EnableR2dbcRepositories
class VixenAuth

fun main(args: Array<String>) {
    runApplication<VixenAuth>(*args)
}