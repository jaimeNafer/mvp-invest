package io.github.jaimenafer.lastro;

import static org.junit.jupiter.api.Assertions.*;

import io.github.jaimenafer.lastro.carteiras.domain.model.Carteira;
import java.util.List;
import org.junit.jupiter.api.Test;

class LombokModelosTests {
  @Test
  void contasVinculadasSaoSnapshotImutavelSemExporColecaoGerenciada() {
    var carteira = Carteira.criar("perfil", "Principal");
    var snapshot = carteira.contasVinculadas();
    assertThrows(UnsupportedOperationException.class, () -> snapshot.add(null));
    carteira.alterar("Principal", List.of(1L));
    assertTrue(snapshot.isEmpty());
    assertEquals(1, carteira.contasVinculadas().size());
    assertThrows(UnsupportedOperationException.class, () -> carteira.contasVinculadas().clear());
  }

  @Test
  void sincronizacaoPreservaVinculoMantidoEEliminaRepeticoes() {
    var carteira = Carteira.criar("perfil", "Principal");
    carteira.alterar("Principal", List.of(1L, 2L));
    var mantida =
        carteira.contasVinculadas().stream()
            .filter(conta -> conta.getInstituicaoId().equals(1L))
            .findFirst()
            .orElseThrow();
    carteira.alterar("Nova", List.of(1L, 3L, 3L));
    assertEquals("Nova", carteira.getNome());
    assertEquals(2, carteira.contasVinculadas().size());
    assertTrue(carteira.contasVinculadas().contains(mantida));
    assertFalse(
        carteira.contasVinculadas().stream()
            .anyMatch(conta -> conta.getInstituicaoId().equals(2L)));
  }
}
