package com.vagamatch.vagaservice.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Regra do match, sem dependência de banco (fácil de testar).
 * Obrigatória pesa 2 e diferencial pesa 1; o nível do candidato não entra,
 * porque a vaga não diz que nível exige.
 * match % = peso das skills que o candidato tem / peso total da vaga.
 */
public final class CalculadoraMatch {

    public static final int PESO_OBRIGATORIA = 2;
    public static final int PESO_DIFERENCIAL = 1;

    private CalculadoraMatch() {
    }

    public static Resultado calcular(Set<Long> skillsDoCandidato, Collection<VagaSkill> skillsDaVaga) {
        int pesoTotal = 0;
        int pesoAtendido = 0;
        List<String> atendidas = new ArrayList<>();
        List<String> obrigatoriasFaltando = new ArrayList<>();
        List<String> diferenciaisFaltando = new ArrayList<>();

        for (VagaSkill vs : skillsDaVaga) {
            int peso = peso(vs.isObrigatoria());
            pesoTotal += peso;
            if (skillsDoCandidato.contains(vs.getSkill().getId())) {
                pesoAtendido += peso;
                atendidas.add(vs.getSkill().getNome());
            } else if (vs.isObrigatoria()) {
                obrigatoriasFaltando.add(vs.getSkill().getNome());
            } else {
                diferenciaisFaltando.add(vs.getSkill().getNome());
            }
        }

        Comparator<String> porNome = String.CASE_INSENSITIVE_ORDER;
        atendidas.sort(porNome);
        obrigatoriasFaltando.sort(porNome);
        diferenciaisFaltando.sort(porNome);
        return new Resultado(percentual(pesoAtendido, pesoTotal), atendidas, obrigatoriasFaltando, diferenciaisFaltando);
    }

    public static int peso(boolean obrigatoria) {
        return obrigatoria ? PESO_OBRIGATORIA : PESO_DIFERENCIAL;
    }

    /** Arredonda pro inteiro mais próximo; vaga sem skills dá 0. */
    public static int percentual(long pesoAtendido, long pesoTotal) {
        return pesoTotal == 0 ? 0 : (int) Math.round(100.0 * pesoAtendido / pesoTotal);
    }

    public record Resultado(int percentual, List<String> skillsAtendidas,
                            List<String> obrigatoriasFaltando, List<String> diferenciaisFaltando) {

        public boolean atendeTodasObrigatorias() {
            return obrigatoriasFaltando.isEmpty();
        }
    }
}
