package br.com.zup.edu

import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.inject.Singleton

@MicronautTest(transactional = false) // servidor gRPC roa em uma thread separada
internal class CarrosEndpointTest(
    val repository: CarroRepository,
    val grpcClient: CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub
) {

    /*
    * 1. happy test(tudo ok) - ok
    * 2. quando ja existe um carro com a placa
    * 3. quando os dados de entrada sao invalidos
    * */

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
    }

    @Test
    fun `deve adicionar um novo carro`() {

        val request = CarrosRequest.newBuilder()
                                    .setModelo("Gol")
                                    .setPlaca("HPX-1234")
                                    .build()
        val response = grpcClient.adicionar(request)

        with(response) {
            assertNotNull(id)
            assertTrue(repository.existsById(id)) // efeito colateral
        }
    }

    @Test
    fun `nao deve adicionar novo carro quando carro com placa ja existe`() {
        val existente = repository.save(Carro(modelo = "Palio", placa = "OTX-1234"))

        // ação
        val request = CarrosRequest.newBuilder()
                                    .setModelo("Gol")
                                    .setPlaca(existente.placa)
                                    .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.adicionar(request)
        }

        // validação
        with(error) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("carro com placa existente", status.description)
        }
    }

    @Test
    fun `nao deve adicionar novo carro quando dados de entrada for invalido`() {
        // ação
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.adicionar(CarrosRequest.newBuilder().build())
        }

        // validação
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("dados de entrada inválidos", status.description)
        }

    }

    @Factory
    class Clients {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub? {
            return CarrosGrpcServiceGrpc.newBlockingStub(channel)
        }

    }

}