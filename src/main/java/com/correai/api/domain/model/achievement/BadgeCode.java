package com.correai.api.domain.model.achievement;

/**
 * Fixed catalog of achievement badges shown in the "Conquistas" tab. Codes match the asset names
 * bundled with the mobile app (assets/badges/{code}.svg). Unlike Goal challenges, achievements are
 * never re-evaluated as "in progress": once unlocked they stay unlocked forever.
 */
public enum BadgeCode {
    PRIMEIRO_PASSO("primeiro_passo", "Primeiro Passo", "Concluir a primeira atividade"),
    CORRIDA_5K("corrida_5k", "5K", "Completar 5 km em uma única atividade"),
    CORRIDA_10K("corrida_10k", "10K", "Completar 10 km em uma única atividade"),
    MEIA_MARATONA("meia_maratona", "Meia Maratona", "Completar 21,1 km em uma única atividade"),
    MARATONA("maratona", "Maratona", "Completar 42,2 km em uma única atividade"),
    MADRUGADOR("madrugador", "Madrugador", "Treinar antes das 6h da manhã"),
    CORUJA("coruja", "Coruja Noturna", "Treinar às 21h ou mais tarde"),
    SEQUENCIA_7("sequencia_7", "Em Chamas", "Treinar 7 dias seguidos"),
    SEQUENCIA_30("sequencia_30", "Disciplina", "Treinar 30 dias seguidos"),
    REI_DA_MONTANHA("rei_da_montanha", "Rei da Montanha", "Em breve"),
    RELAMPAGO("relampago", "Relâmpago", "Bater uma meta de ritmo médio"),
    CHUVA("chuva", "Faça Chuva", "Em breve"),
    SOL_A_PINO("sol_a_pino", "Sol a Pino", "Treinar entre 11h e 15h"),
    CENTURIAO("centuriao", "Centurião", "Somar 100 km em um mês"),
    EM_GRUPO("em_grupo", "Em Grupo", "Em breve"),
    EXPLORADOR("explorador", "Explorador", "Correr em uma rota nova, longe das rotas já percorridas"),
    CORACAO_FORTE("coracao_forte", "Coração Forte", "Bater a meta semanal de atividades por 4 semanas seguidas"),
    RECORDE_PESSOAL("recorde_pessoal", "Recorde Pessoal", "Bater um recorde pessoal de 5K, 10K, 21K ou 42K"),
    CAMPEAO("campeao", "Campeão", "Em breve");

    private final String code;
    private final String displayName;
    private final String description;

    BadgeCode(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }
}
