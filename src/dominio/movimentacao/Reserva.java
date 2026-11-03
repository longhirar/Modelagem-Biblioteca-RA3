package dominio.movimentacao;

import java.time.LocalDate;

public class Reserva extends Transacao {
    private LocalDate validaAte;
    private boolean prorrogada;

    public Reserva(String id, LocalDate data, String idLivro, String idEstudante, LocalDate validaAte) {
        this(id, data, idLivro, idEstudante, validaAte, false);
    }

    public Reserva(String id, LocalDate data, String idLivro, String idEstudante, LocalDate validaAte,
                   boolean prorrogada) {
        super(id, data, idLivro, idEstudante);
        this.validaAte = validaAte;
        this.prorrogada = prorrogada;
    }

    public boolean pertenceA(String idEstudante) {
        return getIdEstudante().equals(idEstudante);
    }

    // a reserva vale até o fim do dia validaAte
    public boolean vencidaEm(LocalDate hoje) {
        return hoje.isAfter(validaAte);
    }

    // guarda da prorrogação: só uma vez e só enquanto a reserva ainda vale
    public boolean podeProrrogar(LocalDate hoje) {
        return !prorrogada && !vencidaEm(hoje);
    }

    // ação da prorrogação
    public void prorrogar(int dias) {
        this.validaAte = validaAte.plusDays(dias);
        this.prorrogada = true;
    }

    public LocalDate getValidaAte() { return validaAte; }
    public boolean isProrrogada() { return prorrogada; }

    @Override
    public String toString() {
        return "Reserva " + getIdTransacao() + " livro=" + getIdLivro() + " estudante=" + getIdEstudante()
                + " valida ate " + validaAte + (prorrogada ? " (prorrogada)" : "");
    }
}
