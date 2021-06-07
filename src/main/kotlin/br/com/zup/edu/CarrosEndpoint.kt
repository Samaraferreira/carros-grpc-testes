package br.com.zup.edu

import io.grpc.Status
import io.grpc.stub.StreamObserver
import javax.inject.Singleton
import javax.validation.ConstraintViolationException

@Singleton
class CarrosEndpoint(val repository: CarroRepository): CarrosGrpcServiceGrpc.CarrosGrpcServiceImplBase() {

    override fun adicionar(request: CarrosRequest, responseObserver: StreamObserver<CarrosResponse>) {

        if (repository.existsByPlaca(request.placa)) {
            responseObserver.onError(Status.ALREADY_EXISTS
                .withDescription("carro com placa existente")
                .asRuntimeException())
            return
        }

        val carro = Carro(modelo = request.modelo, placa = request.placa)

        try {
            repository.save(carro)
        } catch (e: ConstraintViolationException) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription("dados de entrada inválidos")
                .asRuntimeException())
            return
        }

        responseObserver.onNext(CarrosResponse.newBuilder().setId(carro.id!!).build())
        responseObserver.onCompleted()
    }

}