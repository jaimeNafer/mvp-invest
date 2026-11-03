package io.github.jaimenafer.lastro.carteiras.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CadastroCarteira(
    @NotBlank(message = "Informe o nome da carteira.")
    @Size(max = 160, message = "Informe um nome de carteira com até 160 caracteres.") String nome,
    @NotEmpty(message = "Selecione pelo menos uma instituição.")
    List<@NotNull(message = "Informe uma instituição válida.")
         @Positive(message = "Informe uma instituição válida.") Long> instituicaoIds) {
  public CadastroCarteira {
    nome = nome == null ? null : nome.strip();
  }
}
