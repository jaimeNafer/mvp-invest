package io.github.jaimenafer.lastro.instituicoes;

import java.util.Collection;
import java.util.List;
import org.springframework.data.repository.Repository;

public interface InstituicaoRepository extends Repository<Instituicao, Long> {
    List<Instituicao> findAllByOrderByNomeAscIdAsc();
    List<Instituicao> findAllByIdIn(Collection<Long> ids);
}
