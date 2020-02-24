package auth.vixen

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import xyz.downgoon.snowflake.Snowflake

@Repository
class Database {
    @Autowired
    private lateinit var client: DatabaseClient

    fun createUser(username: String, email: String, password: String): Mono<String> {
        return client.execute("""
INSERT INTO users (snowflake, email, hash, username)
SELECT :snowflake, :email, crypt(:password, gen_salt('bf')), :username
WHERE NOT exists(
        SELECT * FROM users WHERE email = :email
    )
RETURNING jsonb_build_object(
        'snowflake', snowflake
    )::text json
        """)
                .bind("snowflake", Snowflake(1, 1).nextId().toString())
                .bind("email", email)
                .bind("password", password)
                .bind("username", username)
                .fetch()
                .first()
                .map { it["json"] as String }
    }

    fun createToken(email: String, password: String): Mono<String> {
        return client.execute("""
INSERT INTO tokens (snowflake, token)
SELECT users.snowflake, encode(gen_random_uuid()::text::bytea, 'base64')
FROM users
WHERE email = :email
  AND hash = crypt(:password, hash)
RETURNING token
        """)
                .bind("email", email)
                .bind("password", password)
                .fetch()
                .first()
                .map { it["token"] as String }
    }

    fun deleteToken(token: String): Mono<Void> {
        return client.execute("""
DELETE
FROM tokens
WHERE token = :token
        """)
                .bind("token", token)
                .fetch()
                .first()
                .then()
    }

    fun getSelf(token: String): Mono<String> {
        return client.execute("""
SELECT jsonb_build_object(
               'snowflake', users.snowflake,
               'email', users.email,
               'username', users.username
           )::text json
FROM users
WHERE users.snowflake = (SELECT snowflake FROM tokens WHERE token = :token)
GROUP BY users.snowflake
        """)
                .bind("token", token)
                .fetch()
                .first()
                .map { it["json"] as String }
    }
}