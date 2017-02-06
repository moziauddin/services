package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.Author
import au.org.biodiversity.nsl.AuthorService
import grails.transaction.Transactional
import org.apache.shiro.authz.annotation.RequiresRoles
import org.grails.plugins.metrics.groovy.Timed
import org.springframework.http.HttpStatus

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import static org.springframework.http.HttpStatus.OK

@Transactional
class AuthorController implements UnauthenticatedHandler, WithTarget {
    static namespace = "api"

    static responseFormats = [
            index      : ['html'],
            deduplicate: ['json', 'xml', 'html']
    ]

    static allowedMethods = [
            deduplicate: ["DELETE"]
    ]

    AuthorService authorService
    def jsonRendererService

    def index() {}

    @Timed
    @RequiresRoles('admin')
    def deduplicate(long id, long target) {
        Author targetAuthor = Author.get(target)
        Author duplicate = Author.get(id)
        withTargets(["Target author": targetAuthor, "Duplicate author": duplicate]) { ResultObject result ->
            if (duplicate.id == targetAuthor.id) {
                result.error("Duplicate and Target author are the same.")
                result.status = HttpStatus.BAD_REQUEST
                result.ok = false
                return
            }
            try {
                authorService.deduplicate(duplicate, targetAuthor)
                result.status = OK
                result.ok = true
            } catch (e) {
                e.printStackTrace()
                result.error "Could not deduplicate: $e.message"
                result.status = INTERNAL_SERVER_ERROR
                result.ok = false
            }
        }
    }
}