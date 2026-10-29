package br.pucpr.airsense.users

import br.pucpr.airsense.security.UserToken
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
class UserController(private val userService: UserService) {

    @PostMapping
    fun create(@Valid @RequestBody req: CreateUserRequest): ResponseEntity<UserResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(userService.create(req))

    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest): ResponseEntity<LoginResponse> =
        ResponseEntity.ok(userService.login(req))

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal user: UserToken): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.findById(user.id, user))

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun findAll(): ResponseEntity<List<UserResponse>> =
        ResponseEntity.ok(userService.findAll())

    @GetMapping("/{id}")
    fun findById(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.findById(id, user))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody req: UpdateUserRequest,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.update(id, req, user))

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: UserToken
    ): ResponseEntity<Void> {
        userService.delete(id, user)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{id}/roles/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    fun addRole(
        @PathVariable id: Long,
        @PathVariable role: String
    ): ResponseEntity<UserResponse> =
        ResponseEntity.ok(userService.addRole(id, role))
}
