package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import jakarta.validation.constraints.*;
import java.util.*;

public record OrigemRegistro(
    Map<String, String> campos,
    List<String> erros,
    String operacao,
    String moeda,
    String origem,
    String arquivoRef,
    Integer linha,
    String aba) {}
