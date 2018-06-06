/*
 * Copyright 2017-2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.http.client

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.context.ApplicationContext
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.StreamRequestSpec.Book
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.server.EmbeddedServer
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import io.reactivex.FlowableOnSubscribe
import io.reactivex.Single
import io.reactivex.annotations.NonNull
import io.reactivex.functions.Function
import spock.lang.AutoCleanup
import spock.lang.IgnoreIf
import spock.lang.Shared
import spock.lang.Specification
import java.nio.charset.StandardCharsets

/**
 * @author graemerocher
 * @since 1.0
 */
@IgnoreIf({System.getenv("TRAVIS")})
class StreamRequestSpec extends Specification {
    @Shared @AutoCleanup EmbeddedServer embeddedServer =
            ApplicationContext.run(EmbeddedServer)

    void "test stream post request with strings"() {
        given:
        RxHttpClient client = RxHttpClient.create(embeddedServer.getURL())

        when:
        int i = 0
        HttpResponse<List> result = client.exchange(HttpRequest.POST('/stream/request/strings', Flowable.create( new FlowableOnSubscribe<Object>() {
            @Override
            void subscribe(@NonNull FlowableEmitter<Object> emitter) throws Exception {
                while(i < 5) {
                    emitter.onNext("Number ${i++}")
                }
                emitter.onComplete()


            }
        }, BackpressureStrategy.BUFFER

        )).contentType(MediaType.TEXT_PLAIN_TYPE), List).blockingFirst()

        then:
        result.body().size() == 5
        result.body() == ["Number 0", "Number 1", "Number 2", "Number 3", "Number 4"]

        cleanup:
        client.stop()
        client.close()
    }

    void "test stream post request with byte chunks"() {
        given:
        RxHttpClient client = RxHttpClient.create(embeddedServer.getURL())

        when:
        int i = 0
        HttpResponse<List> result = client.exchange(HttpRequest.POST('/stream/request/bytes', Flowable.create( new FlowableOnSubscribe<Object>() {
            @Override
            void subscribe(@NonNull FlowableEmitter<Object> emitter) throws Exception {
                while(i < 5) {
                    emitter.onNext("Number ${i++}".getBytes(StandardCharsets.UTF_8))
                }
                emitter.onComplete()


            }
        }, BackpressureStrategy.BUFFER

        )).contentType(MediaType.TEXT_PLAIN_TYPE), List).blockingFirst()

        then:
        result.body().size() == 5
        result.body() == ["Number 0", "Number 1", "Number 2", "Number 3", "Number 4"]

        cleanup:
        client.stop()
        client.close()
    }

    void "test stream post request with POJOs"() {
        given:
        RxHttpClient client = RxHttpClient.create(embeddedServer.getURL())

        when:
        int i = 0
        HttpResponse<List> result = client.exchange(HttpRequest.POST('/stream/request/pojos', Flowable.create( new FlowableOnSubscribe<Object>() {
            @Override
            void subscribe(@NonNull FlowableEmitter<Object> emitter) throws Exception {
                while(i < 5) {
                    emitter.onNext(new Book(title:"Number ${i++}"))
                }
                emitter.onComplete()


            }
        }, BackpressureStrategy.BUFFER

        )), Argument.of(List, Book)).blockingFirst()

        then:
        result.body().size() == 5
        result.body() == [new Book(title: "Number 0"), new Book(title: "Number 1"), new Book(title: "Number 2"), new Book(title: "Number 3"), new Book(title: "Number 4")]

        cleanup:
        client.stop()
        client.close()
    }

    void "test stream post request with POJOs flowable"() {
        given:
        RxHttpClient client = RxHttpClient.create(embeddedServer.getURL())

        when:
        int i = 0
        HttpResponse<List> result = client.exchange(HttpRequest.POST('/stream/request/pojo-flowable', Flowable.create( new FlowableOnSubscribe<Object>() {
            @Override
            void subscribe(@NonNull FlowableEmitter<Object> emitter) throws Exception {
                while(i < 5) {
                    emitter.onNext(new Book(title:"Number ${i++}"))
                }
                emitter.onComplete()

            }
        }, BackpressureStrategy.BUFFER

        )), Argument.of(List, Book)).blockingFirst()

        then:
        result.body().size() == 5
        result.body() == [new Book(title: "Number 0"), new Book(title: "Number 1"), new Book(title: "Number 2"), new Book(title: "Number 3"), new Book(title: "Number 4")]

        cleanup:
        client.stop()
        client.close()
    }

    void "test json stream post request with POJOs flowable"() {
        given:
        RxStreamingHttpClient client = RxStreamingHttpClient.create(embeddedServer.getURL())

        when:
        int i = 0
        List<Book> result = client.jsonStream(HttpRequest.POST('/stream/request/pojo-flowable', Flowable.create( new FlowableOnSubscribe<Object>() {
            @Override
            void subscribe(@NonNull FlowableEmitter<Object> emitter) throws Exception {
                while(i < 5) {
                    emitter.onNext(new Book(title:"Number ${i++}"))
                }
                emitter.onComplete()

            }
        }, BackpressureStrategy.BUFFER

        )), Book).toList().blockingGet()

        then:
        result.size() == 5
        result == [new Book(title: "Number 0"), new Book(title: "Number 1"), new Book(title: "Number 2"), new Book(title: "Number 3"), new Book(title: "Number 4")]

        cleanup:
        client.close()
    }

    void "test json stream post request with POJOs flowable error"() {
        given:
        RxStreamingHttpClient client = RxStreamingHttpClient.create(embeddedServer.getURL())

        when:
        int i = 0
        List<Book> result = client.jsonStream(HttpRequest.POST('/stream/request/pojo-flowable-error', Flowable.create( new FlowableOnSubscribe<Object>() {
            @Override
            void subscribe(@NonNull FlowableEmitter<Object> emitter) throws Exception {
                while(i < 5) {
                    emitter.onNext(new Book(title:"Number ${i++}"))
                }
                emitter.onComplete()

            }
        }, BackpressureStrategy.BUFFER

        )), Book).toList().blockingGet()

        then:
        def e= thrown(RuntimeException) // TODO: this should be HttpClientException
        e != null

        cleanup:
        client.close()
    }
    @Controller('/stream/request')
    static class StreamController {

        @Post(consumes = MediaType.TEXT_PLAIN)
        Single<List<String>> strings(@Body Flowable<String> strings) {
            strings.toList()
        }

        @Post(consumes = MediaType.TEXT_PLAIN)
        Single<List<String>> bytes(@Body Flowable<byte[]> strings) {
            strings.map({ byte[] bytes -> new String(bytes, StandardCharsets.UTF_8)}).toList()
        }

        @Post
        Single<List<Book>> pojos(@Header MediaType contentType, @Body Single<List<Book>> books) {
            assert contentType == MediaType.APPLICATION_JSON_TYPE
            books
        }

        @Post
        Flowable<Book> pojoFlowable(@Header MediaType contentType, @Body Flowable<Book> books) {
            assert contentType == MediaType.APPLICATION_JSON_TYPE
            books
        }

        @Post
        Flowable<Book> pojoFlowableError(@Header MediaType contentType, @Body Flowable<Book> books) {
            return books.flatMap({ Book book ->
                if(book.title.endsWith("3")) {
                    return Flowable.error(new RuntimeException("Can't have books with 3"))
                }
                else {
                    return Flowable.just(book)
                }
            })
        }
    }

    @EqualsAndHashCode
    @ToString(includePackage = false)
    static class Book {
        String title
    }
}