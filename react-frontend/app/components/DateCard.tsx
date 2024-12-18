import MaterialTailwind from "@material-tailwind/react";
const {
    Card,
    CardBody,
    CardFooter,
    Typography,
    Button, CardHeader, Accordion, AccordionBody, IconButton,
} = MaterialTailwind;
import React from "react";

export function DateCard({title, start, end, startShort, ort}: {title: string, start: string, end: string, startShort: string, ort: string}) {
    const [open, setOpen] = React.useState(0);

    const handleOpen = (value: number) => setOpen(open === value ? 0 : value);

    return (
        <Card className="m-2 w-96 bg-gradient-to-bl from-gray-900/85 to-gray-900/80 h-fit flex-grow border-gray-700 border">
            <CardHeader floated={false} className="bg-gray-900 border-gray-700 border" onClick={() => handleOpen(1)}>
                <Typography variant="h5" color="white" className="m-2">
                    {title}  {startShort} <IconButton variant="text" color={"white"}><span class="material-icons">edit</span></IconButton>
                </Typography>
            </CardHeader>
            <CardBody className="mt-0 py-2">
                <Accordion open={open === 1} className="p-0 m-0">
                    <AccordionBody className="p-0 m-0">
                        <Typography className="text-white font-bold">
                            Dieser Termin startet am {start} und geht bis zum {end}
                        </Typography>
                        <Typography className="text-white font-bold">
                            Ort: {ort}
                        </Typography>
                    </AccordionBody>
                </Accordion>
            </CardBody>
            <CardFooter className="pt-0 flex flex-row justify-between">
                <Button variant={"outlined"} color={"white"} className="border-blue-600">Bin dabei</Button>
                <Button variant={"outlined"} color={"white"} className="border-blue-600">Bin raus</Button>
            </CardFooter>
        </Card>
    );
}