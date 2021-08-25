package com.example.springjpa.many_to_one.one_way;

import com.example.springjpa.many_to_one.Member;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * Car(N) -> Member(N)
 *
 *     create table car (
 *        car_id bigint generated by default as identity,
 *         member_id bigint,
 *         primary key (car_id)
 *     )
 */

@Getter
@Setter
@Entity
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "car_id")
    private Long id;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "member_id")
    private Member member;
}
