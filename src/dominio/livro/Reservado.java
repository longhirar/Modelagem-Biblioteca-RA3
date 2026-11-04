package dominio.livro;

import dominio.Ids;
import dominio.RegrasCirculacao;
import dominio.movimentacao.Emprestimo;
import dominio.movimentacao.Reserva;

import java.time.LocalDate;

// Padrão State - estado concreto RESERVADO
public final class Reservado extends EstadoLivro {
    public static final String NOME = "RESERVADO";
    public static final Reservado INSTANCIA = new Reservado();

    private Reservado() {
    }

    @Override
    public String nome() { return NOME; }

    // emprestarLivro [mesmo estudante da reserva && reserva valida] / calcularDataDevolucao -> EMPRESTADO
    @Override
    public Emprestimo emprestar(Livro livro, Reserva reserva, String idEstudante, LocalDate hoje) {
        if (reserva == null || !reserva.getIdLivro().equals(livro.getId()) || !reserva.pertenceA(idEstudante)) {
            throw new TransicaoRecusadaException("emprestarLivro", NOME,
                    "o livro esta reservado para outro estudante");
        }
        if (reserva.vencidaEm(hoje)) {
            throw new ReservaVencidaException(NOME, reserva.getValidaAte());
        }
        Emprestimo emprestimo = new Emprestimo(Ids.novo("EMP"), hoje, livro.getId(), idEstudante,
                hoje.plusDays(RegrasCirculacao.PRAZO_EMPRESTIMO_DIAS), 0, true);
        livro.alterarEstado(Emprestado.INSTANCIA);
        return emprestimo;
    }

    // prorrogarReserva [mesmo estudante && ainda valida && nunca prorrogada] / validaAte += 7 -> continua RESERVADO
    // a guarda vem antes da ação: se for recusada, a validade não muda
    @Override
    public void prorrogarReserva(Livro livro, Reserva reserva, String idEstudante, LocalDate hoje) {
        if (reserva == null || !reserva.pertenceA(idEstudante)) {
            throw new TransicaoRecusadaException("prorrogarReserva", NOME, "a reserva nao e deste estudante");
        }
        if (!reserva.podeProrrogar(hoje)) {
            throw new TransicaoRecusadaException("prorrogarReserva", NOME, reserva.isProrrogada()
                    ? "a reserva ja foi prorrogada uma vez" : "a reserva ja venceu em " + reserva.getValidaAte());
        }
        reserva.prorrogar(RegrasCirculacao.DIAS_PRORROGACAO_RESERVA);
    }

    // removerReserva -> LIVRE
    @Override
    public void removerReserva(Livro livro) {
        livro.alterarEstado(Livre.INSTANCIA);
    }
}
