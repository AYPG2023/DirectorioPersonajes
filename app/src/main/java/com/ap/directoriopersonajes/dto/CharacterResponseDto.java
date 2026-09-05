package com.ap.directoriopersonajes.dto;

import java.util.List;

public class CharacterResponseDto {

    private List<CharacterDto> items;
    private PaginationMetaDto meta;
    private PaginationLinksDto links;

    public List<CharacterDto> getItems() {
        return items;
    }

    public void setItems(List<CharacterDto> items) {
        this.items = items;
    }

    public PaginationMetaDto getMeta() {
        return meta;
    }

    public void setMeta(PaginationMetaDto meta) {
        this.meta = meta;
    }

    public PaginationLinksDto getLinks() {
        return links;
    }

    public void setLinks(PaginationLinksDto links) {
        this.links = links;
    }
}
