package eu.efti.eftigate.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IdentifiersResults implements Serializable {
    private List<IdentifiersResult> identifiersResult;
}
