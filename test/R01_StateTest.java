import dominio.livro.EstadoLivro;
import dominio.livro.Livro;
import dominio.livro.ReservaVencidaException;
import dominio.livro.TransicaoRecusadaException;
import dominio.movimentacao.Emprestimo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("R01 - Ciclo de vida do livro (State)")
class R01_StateTest {

    @Test
    void R01_terceiraRenovacaoRecusada() {
        Cenario c = new Cenario();
        c.emprestar("L1", "E1");
        c.app.fachada().renovar("L1", "E1");
        c.app.fachada().renovar("L1", "E1");
        LocalDate vencimentoAposDuas = c.emprestimoAtivo("L1").orElseThrow().getDataVencimento();

        TransicaoRecusadaException e = assertThrows(TransicaoRecusadaException.class,
                () -> c.app.fachada().renovar("L1", "E1"));

        Emprestimo emp = c.emprestimoAtivo("L1").orElseThrow();
        assertEquals(vencimentoAposDuas, emp.getDataVencimento(), "o vencimento nao pode mudar");
        assertEquals(2, emp.getNumRenovacoes());
        assertEquals("RENOVADO", c.estadoDoLivro("L1"));
        assertEquals("RENOVADO", e.getEstado());
    }

    @Test
    void R01_livroDevolvidoNaoInspecionadoNaoPodeSerReservado() {
        Cenario c = new Cenario();
        c.emprestar("L1", "E1");
        c.app.fachada().registrarDevolucao("L1", "E1");
        assertEquals("DEVOLVIDO", c.estadoDoLivro("L1"));

        assertThrows(TransicaoRecusadaException.class, () -> c.app.fachada().reservar("L1", "E2"));

        assertEquals("DEVOLVIDO", c.estadoDoLivro("L1"), "o livro nao pode ser alterado");
        assertTrue(c.reservaDoLivro("L1").isEmpty(), "nenhuma reserva pode ter sido gravada");
    }

    @Test
    void R01_reservaValeSeteDias() {
        Cenario c = new Cenario();
        assertEquals(Cenario.HOJE.plusDays(7), c.app.fachada().reservar("L1", "E1").validaAte());
        c.relogio.avancarDias(7);                       // ultimo dia da reserva: ainda vale
        assertEquals("EMPRESTADO", c.app.fachada().efetivarEmprestimo("L1", "E1").estadoLivro());
    }

    @Test
    void R01_reservaVencidaRecusaEmprestimoELiberaOLivro() {
        Cenario c = new Cenario();
        c.app.fachada().reservar("L1", "E1");
        c.relogio.avancarDias(8);

        assertThrows(ReservaVencidaException.class, () -> c.app.fachada().efetivarEmprestimo("L1", "E1"));

        assertEquals("LIVRE", c.estadoDoLivro("L1"), "o livro volta a ficar disponivel");
        assertTrue(c.reservaDoLivro("L1").isEmpty(), "a reserva vencida foi removida");
        assertTrue(c.emprestimoAtivo("L1").isEmpty(), "nenhum emprestimo pode ter sido criado");
    }

    @Test
    void R01_reservaProrrogadaUmaUnicaVezAntesDeVencer() {
        Cenario c = new Cenario();
        c.app.fachada().reservar("L1", "E1");
        c.relogio.avancarDias(5);

        assertEquals(Cenario.HOJE.plusDays(14), c.app.fachada().prorrogarReserva("L1", "E1").validaAte());
        assertThrows(TransicaoRecusadaException.class, () -> c.app.fachada().prorrogarReserva("L1", "E1"));
        assertEquals(Cenario.HOJE.plusDays(14), c.reservaDoLivro("L1").orElseThrow().getValidaAte(),
                "a segunda prorrogacao nao muda a validade");

        c.relogio.avancarDias(9);                       // dia 14: ainda dentro da prorrogacao
        assertEquals("EMPRESTADO", c.app.fachada().efetivarEmprestimo("L1", "E1").estadoLivro());
    }

    @Test
    void R01_reservaJaVencidaNaoPodeSerProrrogada() {
        Cenario c = new Cenario();
        c.app.fachada().reservar("L1", "E1");
        c.relogio.avancarDias(8);

        assertThrows(TransicaoRecusadaException.class, () -> c.app.fachada().prorrogarReserva("L1", "E1"));

        assertEquals(Cenario.HOJE.plusDays(7), c.reservaDoLivro("L1").orElseThrow().getValidaAte());
        assertEquals("RESERVADO", c.estadoDoLivro("L1"));
    }

    @Test
    void R01_laudoIrrecuperavelLevaReparandoAIncinerado() {
        Cenario c = new Cenario();
        c.emprestar("L1", "E1");
        c.app.fachada().registrarDevolucao("L1", "E1");
        assertEquals("REPARANDO", c.app.fachada().inspecionar("L1", false).estadoLivro());
        assertEquals("INCINERADO", c.app.fachada().registrarLaudoIrrecuperavel("L1").estadoLivro());
        // estado terminal: recusa qualquer evento
        assertThrows(TransicaoRecusadaException.class, () -> c.app.fachada().reservar("L1", "E1"));
    }

    @Test
    void R01_reparoConcluidoDevolveOLivroALivre() {
        Cenario c = new Cenario();
        c.emprestar("L1", "E1");
        c.app.fachada().registrarDevolucao("L1", "E1");
        c.app.fachada().inspecionar("L1", false);
        assertEquals("LIVRE", c.app.fachada().concluirReparo("L1").estadoLivro());
    }

    @Test
    void R01_eventoNaoAceitoEhRecusadoSemAlterarOLivro() {
        Livro livro = new Livro("X1", "Titulo", "Autor");
        assertEquals("LIVRE", livro.getEstado());
        assertThrows(TransicaoRecusadaException.class, () -> livro.inspecionar(true));
        assertThrows(TransicaoRecusadaException.class, livro::concluirReparo);
        assertThrows(TransicaoRecusadaException.class, () -> livro.emprestar(null, "E1", LocalDate.now()));
        assertEquals("LIVRE", livro.getEstado());
    }

    @Test
    void R01_estadoGravadoComoTextoEReconstituido() {
        for (String nome : new String[]{"LIVRE", "RESERVADO", "EMPRESTADO", "RENOVADO", "DEVOLVIDO",
                "REPARANDO", "INCINERADO"}) {
            assertEquals(nome, Livro.reconstituir("X", "t", "a", nome).getEstado());
            assertEquals(nome, EstadoLivro.porNome(nome).nome());
        }
        assertThrows(IllegalArgumentException.class, () -> EstadoLivro.porNome("PERDIDO"));
    }
}
