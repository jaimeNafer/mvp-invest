package io.github.jaimenafer.lastro.importacoes.api.controller;

import io.github.jaimenafer.lastro.importacoes.application.dto.ArquivoParaRecebimento;
import io.github.jaimenafer.lastro.importacoes.application.dto.RecebimentoResponse;
import io.github.jaimenafer.lastro.importacoes.application.usecase.IRecebimentoUseCase;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/importacoes/recebimentos")
@RequiredArgsConstructor
public class RecebimentoController {
  private final IRecebimentoUseCase usecase;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public RecebimentoResponse receber(@RequestPart List<MultipartFile> arquivos) throws IOException {
    var entradas = new ArrayList<ArquivoParaRecebimento>();
    for (var arquivo : arquivos)
      entradas.add(new ArquivoParaRecebimento(arquivo.getOriginalFilename(), arquivo.getBytes()));
    return usecase.receber(entradas);
  }
}
