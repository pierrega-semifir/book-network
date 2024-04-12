package fr.piga.booknetwork.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Token {

    @Id
    @GeneratedValue
    private Integer id;
    private String token;
    private LocalDateTime dateCreation;
    private LocalDateTime dateExpiration;
    private LocalDateTime dateValidation;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
