package auth.vixen

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@CrossOrigin(allowCredentials = "true")
@RestController
class Controller(
        private val database: Database
) {
    @RequestMapping(method = [RequestMethod.POST], value = ["/users/create"], consumes = ["application/x-www-form-urlencoded"])
    fun createUser(
            exchange: ServerWebExchange
    ): Mono<ResponseEntity<String>> {
        return exchange.formData.flatMap {
            database
                    .createUser(it["username"]!![0], it["email"]!![0], it["password"]!![0])
                    .map { json -> ResponseEntity.status(HttpStatus.CREATED).body(json) }
                    .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build()))
        }
    }

    @RequestMapping(method = [RequestMethod.POST], value = ["/users/login"], consumes = ["application/x-www-form-urlencoded"])
    fun loginUser(
            exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        return exchange.formData.flatMap {
            database
                    .createToken(it["email"]!![0], it["password"]!![0])
                    .map { token ->
                        ResponseEntity
                                .status(HttpStatus.OK)
                                .header("Set-Cookie", "token=$token; SameSite=Strict; HttpOnly; Secure")
                                .build<Void>()
                    }
                    .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()))
        }
    }

    @RequestMapping(method = [RequestMethod.DELETE], value = ["/users/logout"])
    fun logout(
            @CookieValue("token")
            token: String
    ): Mono<ResponseEntity<Void>> {
        return database
                .deleteToken(token)
                .thenReturn(
                        ResponseEntity
                                .status(HttpStatus.OK)
                                .header("Set-Cookie", "token=null; Expires=0; Max-Age=0")
                                .build()
                )
    }

    @RequestMapping(method = [RequestMethod.GET], value = ["/users/@me"], produces = ["application/json"])
    fun getMe(
            @CookieValue("token")
            token: String?
    ): Mono<ResponseEntity<String>> {
        require(token != null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<String>())
        }

        return database
                .getSelf(token)
                .map { ResponseEntity.status(HttpStatus.OK).body(it) }
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()))
                .onErrorReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build())
    }
}