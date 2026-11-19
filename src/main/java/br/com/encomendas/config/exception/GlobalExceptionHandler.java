package br.com.encomendas.config.exception;

import br.com.encomendas.shared.exception.RecursoNaoEncontradoException;
import br.com.encomendas.shared.exception.RegraNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/** Converte as exceções do domínio em respostas HTTP padronizadas (RFC 7807 / ProblemDetail). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail tratarRecursoNaoEncontrado(RecursoNaoEncontradoException exception, HttpServletRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problema.setTitle("Recurso não encontrado");
        problema.setInstance(URI.create(request.getRequestURI()));
        problema.setProperty("recurso", exception.getRecurso());
        problema.setProperty("identificador", exception.getIdentificador());
        return problema;
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail tratarRegraNegocio(RegraNegocioException exception, HttpServletRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
        problema.setTitle("Regra de negócio violada");
        problema.setInstance(URI.create(request.getRequestURI()));
        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail tratarValidacao(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Erro na validação dos campos informados");
        problema.setTitle("Dados de requisição inválidos");
        problema.setInstance(URI.create(request.getRequestURI()));
        Map<String, String> erros = new HashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(erro -> erros.put(erro.getField(), erro.getDefaultMessage()));
        problema.setProperty("erros", erros);
        return problema;
    }
}
