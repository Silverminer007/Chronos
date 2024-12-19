import React, {type ReactNode} from "react";
import {Avatar, AvatarGroup, Button, Card, CardActions, CardContent, SvgIcon, Typography} from "@mui/material";
import {ThumbDownIcon, ThumbUpIcon} from "~/components/icons";

function getAvatars(people: string[]): ReactNode {
    return (
        <AvatarGroup max={5} sx={{height: 30, width: 30}} spacing={"small"}>
            {
                people.map(person => {
                    return (
                        <Avatar
                            alt={person}
                            sx={{height: 30, width: 30}}
                        >
                        </Avatar>
                    );
                })
            };
        </AvatarGroup>
    );
}

export function DateCard({title, start, end, startShort, ort}: {
    title: string,
    start: string,
    end: string,
    startShort: string,
    ort: string
}) {
    const [open, setOpen] = React.useState(0);

    const handleOpen = (value: number) => setOpen(open === value ? 0 : value);

    return (
        <Card variant="outlined" className="bg-white w-96 h-fit m-2 flex-grow">
            <CardContent>
                <div className={"flex-row flex items-center"}>
                    <Typography variant={"h5"} className={"flex-grow"}>
                        {title}
                    </Typography>
                    {getAvatars(["Jana", "Tatjana", "Jane", "Austin", "Thor", "Mjolnir"])}
                </div>
                <Typography>
                    Montags 17:30 Uhr
                </Typography>
            </CardContent>
            <CardActions className="flex flex-row justify-around w-full">
                <Button variant={"outlined"}>
                    <ThumbUpIcon/>
                    Bin dabei
                </Button>
                <Button variant={"outlined"}>
                    <ThumbDownIcon/>
                    Bin raus
                </Button>
            </CardActions>
        </Card>
    );
    /*
    return (
        <Card className="m-2 w-96 bg-gradient-to-bl from-gray-900/85 to-gray-900/80 h-fit flex-grow border-gray-700 border">
            <CardHeader floated={false} className="bg-gray-900 border-gray-700 border" onClick={() => handleOpen(1)}>
                <Typography variant="h5" color="white" className="m-2">
                    {title}  {startShort} <IconButton variant="text" color={"white"}><span class="material-icons">edit</span></IconButton>
                </Typography>
            </CardHeader>
            <CardContent className="mt-0 py-2">
                <Accordion open={open === 1} className="p-0 m-0">
                    <AccordionDetails className="p-0 m-0">
                        <Typography className="text-white font-bold">
                            Dieser Termin startet am {start} und geht bis zum {end}
                        </Typography>
                        <Typography className="text-white font-bold">
                            Ort: {ort}
                        </Typography>
                    </AccordionDetails>
                </Accordion>
            </CardContent>
            <CardActions className="pt-0 flex flex-row justify-between">
                <Button variant={"outlined"} color={"white"} className="border-blue-600">Bin dabei</Button>
                <Button variant={"outlined"} color={"white"} className="border-blue-600">Bin raus</Button>
            </CardActions>
        </Card>
    );*/
}