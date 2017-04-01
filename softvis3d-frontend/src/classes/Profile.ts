import Scale from "./Scale";
import { observable } from "mobx";

export default class Profile implements SelectOptionValue {

    public description: string;
    @observable
    public heightMetricId: string;
    @observable
    public footprintMetricId: string;
    @observable
    public scale: Scale;

    private id: string;
    private name: string;

    constructor(builder: ProfileBuilder) {
        this.id = builder.id;
        this.name = builder.name;
        this.heightMetricId = builder.heightMetricId;
        this.footprintMetricId = builder.footprintMetricId;
        this.scale = builder.scale;
        this.description = builder.description;
    }

    public getId(): string {
        return this.id;
    }

    public getLabel(): string {
        return this.name;
    }

    public updateConfiguration(footprintMetricId: string, heightMetricId: string, scale: Scale): void {
        this.heightMetricId = heightMetricId;
        this.footprintMetricId = footprintMetricId;
        this.scale = scale;
    }

    public clone(): Profile {
        return new ProfileBuilder(this.id, this.name)
            .withConfiguration(this.footprintMetricId, this.heightMetricId, this.scale)
            .withDescription(this.description)
            .build();
    }
}

export class ProfileBuilder {

    public id: string;
    public name: string;
    public heightMetricId: string;
    public footprintMetricId: string;
    public scale: Scale;
    public description: string;

    constructor(id: string, name: string) {
        this.id = id;
        this.name = name;
    }

    public withConfiguration(footprintMetricId: string, heightMetricId: string, scale: Scale): ProfileBuilder {
        this.heightMetricId = heightMetricId;
        this.footprintMetricId = footprintMetricId;
        this.scale = scale;
        return this;
    }

    public withDescription(description: string): ProfileBuilder {
        this.description = description;
        return this;
    }

    public build(): Profile {
        return new Profile(this);
    }

}