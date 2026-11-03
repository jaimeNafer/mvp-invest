package io.github.jaimenafer.lastro.carteiras.application.dto;

import java.util.List;

public record CarteiraResponse(String id, String nome, boolean ativo, List<ContaResponse> contas) {}
