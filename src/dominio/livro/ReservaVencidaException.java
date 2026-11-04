package dominio.livro;

import java.time.LocalDate;

// Guarda [reserva valida] do emprestarLivro falsa: a reserva passou do prazo.
// O estado não muda aqui (evento recusado sem alterar o livro). Quem recebe esta exceção
// (a FachadaCirculacao) libera o livro numa operação separada (removerReserva).
public class ReservaVencidaException extends TransicaoRecusadaException {
    public ReservaVencidaException(String estado, LocalDate validaAte) {
        super("emprestarLivro", estado, "reserva vencida em " + validaAte + "; a reserva foi removida e o livro liberado");
    }
}
