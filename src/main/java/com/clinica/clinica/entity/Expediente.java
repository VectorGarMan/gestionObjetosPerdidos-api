package com.clinica.clinica.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Entity
@Table(name = "expedientes")
public class Expediente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "diagnostico_inicial", length = Integer.MAX_VALUE)
    private String diagnosticoInicial;

    @Column(name = "grupo_sanguineo", nullable = false)
    private Long grupoSanguineo;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Expediente paciente;


}