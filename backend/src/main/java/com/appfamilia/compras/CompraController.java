package com.appfamilia.compras;

import com.appfamilia.compras.perfil.PerfilComprasService;
import com.appfamilia.compras.perfil.ProdutoCasa.Evento;
import com.appfamilia.shared.erro.ErroNegocio;
import com.appfamilia.shared.seguranca.UsuarioAtual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lista de compras da Casa. Adicionar e marcar como comprado alimentam o perfil de compras,
 * que ajusta as sugestões por foto (CompraFotoController).
 */
@RestController
@RequestMapping("/api/compras")
public class CompraController {

    public record NovoItem(@NotBlank @Size(max = 160) String nome, @Size(max = 60) String quantidade) {
    }

    public record Marcacao(boolean comprado) {
    }

    public record ItemResposta(UUID id, String nome, String quantidade, boolean comprado) {
        static ItemResposta de(ItemCompra i) {
            return new ItemResposta(i.getId(), i.getNome(), i.getQuantidade(), i.isComprado());
        }
    }

    private final ItemCompraRepository itens;
    private final PerfilComprasService perfil;

    public CompraController(ItemCompraRepository itens, PerfilComprasService perfil) {
        this.itens = itens;
        this.perfil = perfil;
    }

    @GetMapping
    public List<ItemResposta> listar(@AuthenticationPrincipal Jwt jwt) {
        return itens.findByCasaIdOrderByCompradoAscCriadoEmAsc(UsuarioAtual.de(jwt).casaId()).stream()
                .map(ItemResposta::de)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ItemResposta criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody NovoItem dados) {
        String quantidade = dados.quantidade() == null || dados.quantidade().isBlank() ? null : dados.quantidade().trim();
        UUID casaId = UsuarioAtual.de(jwt).casaId();
        perfil.registrar(casaId, dados.nome(), null, Evento.ADICIONADO);
        return ItemResposta.de(itens.save(new ItemCompra(casaId, dados.nome().trim(), quantidade)));
    }

    @PutMapping("/{id}/comprado")
    @Transactional
    public ItemResposta marcar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @RequestBody Marcacao dados) {
        ItemCompra item = buscar(jwt, id);
        if (dados.comprado() && !item.isComprado()) {
            perfil.registrar(UsuarioAtual.de(jwt).casaId(), item.getNome(), null, Evento.COMPRADO);
        }
        item.setComprado(dados.comprado());
        return ItemResposta.de(item);
    }

    /** Remove da lista tudo o que já foi comprado. */
    @DeleteMapping("/comprados")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void limparComprados(@AuthenticationPrincipal Jwt jwt) {
        itens.deleteAll(itens.findByCasaIdAndCompradoTrue(UsuarioAtual.de(jwt).casaId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void remover(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        itens.delete(buscar(jwt, id));
    }

    private ItemCompra buscar(Jwt jwt, UUID id) {
        return itens.findByIdAndCasaId(id, UsuarioAtual.de(jwt).casaId())
                .orElseThrow(() -> new ErroNegocio(HttpStatus.NOT_FOUND, "Item não encontrado"));
    }
}
